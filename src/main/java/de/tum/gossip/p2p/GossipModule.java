package de.tum.gossip.p2p;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import de.tum.gossip.ConfigurationFile;
import de.tum.gossip.api.APIConnection;
import de.tum.gossip.api.packets.APIPacketGossipNotification;
import de.tum.gossip.crypto.GossipCrypto;
import de.tum.gossip.crypto.HostKey;
import de.tum.gossip.crypto.SelfSignedCertifyingTrustManager;
import de.tum.gossip.crypto.certificates.HostKeyCertificate;
import de.tum.gossip.crypto.certificates.HostKeySelfSignedX509Certificates;
import de.tum.gossip.net.ProtocolDescription;
import de.tum.gossip.net.TCPClient;
import de.tum.gossip.net.TCPServer;
import de.tum.gossip.net.util.ChannelCloseReason;
import de.tum.gossip.p2p.packets.*;
import de.tum.gossip.p2p.protocol.EstablishedSession;
import de.tum.gossip.p2p.protocol.GossipClientHandshakeListener;
import de.tum.gossip.p2p.protocol.GossipServerHandshakeListener;
import de.tum.gossip.p2p.util.DataType;
import de.tum.gossip.p2p.util.GossipMessage;
import de.tum.gossip.p2p.util.GossipMessageId;
import de.tum.gossip.p2p.util.MessageNotificationId;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by Andi on 05.07.22.
 */
public class GossipModule {
    private static final ProtocolDescription PROTOCOL = new ProtocolDescription()
            .registerInboundAndOutbound(1, GossipPacketHandshakeHello::new)
            .registerInboundAndOutbound(2, GossipPacketHandshakeIdentityVerification1::new)
            .registerInboundAndOutbound(3, GossipPacketHandshakeIdentityVerification2::new)
            .registerInboundAndOutbound(15, GossipHandshakeComplete::new)
            .registerInboundAndOutbound(16, GossipPacketSpreadKnowledge::new)
            .registerInboundAndOutbound(126, GossipPacketDisconnect::new);

    private final Logger logger = LogManager.getLogger(GossipModule.class);
    private final EventLoopGroup eventLoopGroup;


    public final HostKey hostKey;
    private final TCPServer server;

    /**
     * All TCPClients >we< created to initiate a GossipEstablishedSession.
     */
    private final List<TCPClient> clients;
    private final Lock clientListLock = new ReentrantLock();

    public final PeerIdentityStorage identityStorage;

    /**
     * The maximum amount of sessions we try to maintain.
     */
    private final int networkDegree;
    /**
     * The list of established sessions with other peers in the network.
     */
    private final Set<EstablishedSession> sessionList;
    private final ReadWriteLock sessionListLock = new ReentrantReadWriteLock();

    /**
     * / For every DataType, we capture the set of API connections registered to receive notifications.
     */
    private final Map<DataType, Set<APIConnection>> messageNotificationRegistrations = Maps.newConcurrentMap();
    /**
     * / For every API connection, we track the set of DataTypes they are registered to.
     */
    private final Map<APIConnection, Set<DataType>> dataTypeRegistrations = Maps.newHashMap();
    /**
     * RW lock, synchronizing access to the above two data structures.
     */
    private final ReadWriteLock registrationLock = new ReentrantReadWriteLock();

    /**
     * For messages, we await validation for, we need to maintain a translation from notification id (used within the API-layer protocol)
     * to the message id (used withing the P2P-layer protocol).
     */
    private final ConcurrentMap<MessageNotificationId, GossipMessageId> messageIdTranslation;
    /**
     * Current knowledge propagating within the system!
     * May contain not yet validated messages!
     */
    private final Cache<GossipMessageId, GossipMessage> gossipKnowledgeBase;


    public GossipModule(ConfigurationFile configuration, EventLoopGroup eventLoopGroup) {
        File file = new File(configuration.hostkey());
        Preconditions.checkArgument(file.exists(), "Hostkey located at " + file.getAbsolutePath() + " doesn't exist!");

        this.eventLoopGroup = eventLoopGroup;
        this.identityStorage = new PeerIdentityStorage(); // TODO make path configurable for tests!
        this.hostKey = GossipCrypto.readHostKey(file);
        this.server = newServerProtocol().makeServer(configuration.p2p_address(), configuration.p2p_port(), eventLoopGroup, () -> new GossipServerHandshakeListener(this));

        this.networkDegree = configuration.degree();

        this.clients = Lists.newArrayListWithCapacity(configuration.degree());
        this.sessionList = Sets.newHashSetWithExpectedSize(configuration.degree());

        this.messageIdTranslation = Maps.newConcurrentMap();
        this.gossipKnowledgeBase = Caffeine.newBuilder()
                .maximumSize(configuration.cache_size())
                .evictionListener((key, value, cause) -> {
                    Preconditions.checkState(cause != RemovalCause.COLLECTED, "Encountered unexpected COLLECTED cause");
                    Preconditions.checkNotNull(value); // can't be null, as removal cause will never be COLLECTED
                    var notificationId = ((GossipMessage) value).notificationId.get();
                    if (notificationId != null) {
                        messageIdTranslation.remove(notificationId);
                    }
                })
                .build();
    }

    public ChannelFuture run() {
        return server.bind().addListener(future -> {
            if (future.isSuccess()) {
                logger.info("Gossip server listening on {}:{}", server.hostname, server.port);
            }
        });

        // TODO start server with a fixed seed of known clients!!!
        //  connect to known members at random (unavailable reduces the count?, reties, until we reached maximum degree)!
    }

    public ChannelFuture shutdown() {
        var handle = server.getHandle();
        Preconditions.checkNotNull(handle);

        ChannelPromise promise = handle.newPromise();
        AtomicInteger integer = new AtomicInteger(0);
        GenericFutureListener<? extends Future<? super Void>> decrement = future -> {
            int result = integer.decrementAndGet();
            if (result <= 0) {
                promise.setSuccess();
            }
        };

        clientListLock.lock();
        try {
            integer.addAndGet(1); // server listener
            integer.addAndGet(clients.size());

            server.stop().addListener(decrement);

            for (var client: clients) {
                client.disconnect().addListener(decrement);
            }
        } finally {
            clientListLock.unlock();
        }

        return promise;
    }

    public Future<TCPClient> newClientConnection(GossipPeerInfo remotePeerInfo, String hostname, int port) {
        logger.debug("Trying to connect to new peer at {}:{}", hostname, port);

        var client = newClientProtocol(remotePeerInfo).makeClient(hostname, port, eventLoopGroup, () -> new GossipClientHandshakeListener(this, remotePeerInfo));

        Promise<TCPClient> clientPromise = eventLoopGroup.next().newPromise();

        client.connect().addListener(future -> {
            if (future.isCancelled()) {
                clientPromise.cancel(false);
            } else if (!future.isSuccess()) {
                // TODO implement retry logic in outer method!
                clientPromise.setFailure(future.cause());
            } else {
                logger.debug("Successfully connected to new peer at {}:{}", hostname, port);

                clientListLock.lock();
                try {
                    clients.add(client);
                } finally {
                    clientListLock.unlock();
                }

                clientPromise.setSuccess(client);
            }
        });

        return clientPromise;
    }

    public Optional<ChannelCloseReason> registerEstablishedSession(EstablishedSession session) {
        sessionListLock.readLock().lock();
        try {
            if (sessionList.size() >= networkDegree) {
                return Optional.of(new GossipPacketDisconnect.OutboundCloseReason(
                        GossipPacketDisconnect.Reason.BUSY,
                        "Reached maximum network degree of " + networkDegree + "!"
                ));
            } else if (sessionList.contains(session)) {
                // check if we already have a session to this node. EstablishedSession implements hasCode and equals
                // methods based on the remote peer info.
                return Optional.of(new GossipPacketDisconnect.OutboundCloseReason(
                        GossipPacketDisconnect.Reason.DUPLICATE,
                        "Found same peer already in the list of established sessions!"
                ));
            } else if (session.peerInfo().identity().equals(hostKey.identity)) {
                logger.fatal("Remote peer tried to connect with our local identity!");
                return Optional.of(new GossipPacketDisconnect.OutboundCloseReason(
                        GossipPacketDisconnect.Reason.DUPLICATE,
                        "Disallowed to connect with self identity!"
                ));
            }
        } finally {
            sessionListLock.readLock().unlock();
        }

        // TODO do ip based rate limiting?
        sessionListLock.writeLock().lock();
        try {
            sessionList.add(session);
        } finally {
            sessionListLock.writeLock().unlock();
        }

        // TODO log with some session id?: logger.info("Established new peer session with ");

        // TODO shall new connections receive messages we currently still have queued?

        return Optional.empty();
    }

    public void handleSessionDisconnect(EstablishedSession session) {
        sessionListLock.writeLock().lock();
        try {
            sessionList.remove(session);
        } finally {
            sessionListLock.writeLock().unlock();
        }
    }

    public void registerNotification(APIConnection connection, DataType dataType) {
        // we assume friendly API connections (they are completely unprotected), so we don't impose any
        // rate limiting or whatsoever!

        registrationLock.writeLock().lock();
        try {
            Set<APIConnection> connections = messageNotificationRegistrations
                    .computeIfAbsent(dataType, key -> Sets.newHashSet());
            connections.add(connection);

            Set<DataType> dataTypes = dataTypeRegistrations
                    .computeIfAbsent(connection, key -> Sets.newHashSet());
            dataTypes.add(dataType);
        } finally {
            registrationLock.writeLock().unlock();
        }

        logger.info("[{}] API connected module registered to data type {}!", connection.name(), dataType);
        // TODO should we send them all the previously valid data?
    }

    public void handleDisconnectedAPIClient(APIConnection connection) {
        registrationLock.writeLock().lock();
        try {
            Set<DataType> dataTypes = dataTypeRegistrations.remove(connection);
            if (dataTypes == null) {
                return;
            }

            for (var dataType : dataTypes) {
                Set<APIConnection> connections = messageNotificationRegistrations.get(dataType);
                if (connections != null) {
                    connections.remove(connection);
                }
            }
        } finally {
            registrationLock.writeLock().unlock();
        }

        logger.info("[{}] API connected module disconnected!", connection.name());
    }


    public void spreadInformation(APIConnection originator, int ttl, DataType dataType, byte[] data) throws GossipException {
        GossipMessage gossipMessage;

        registrationLock.readLock().lock();
        try {
            var registeredClients = messageNotificationRegistrations.get(dataType);

            if (registeredClients == null || !registeredClients.contains(originator)) {
                // originator is not in the set of clients which have notification enabled for this data type.

                logger.warn("[{}] API connected module tried to spread knowledge of type {} while it itself wasn't subscribed to it!", originator, dataType);
                throw new GossipException(GossipException.Type.UNSUBSCRIBED_SPREAD);
            }

            gossipMessage = allocateGossipMessage(ttl, dataType, data);

            logger.debug("[{}] API connected module requested to spread {} bytes of type {} with a reach of {} hops. Identified by {}.",
                    originator.name(), data.length, dataType, ttl, gossipMessage.messageId);

            if (registeredClients.size() > 1) {
                // we have other clients locally which are registered for this message.
                // we spread information to them first and then continue to spread the data in the network
                // once they report data validity.

                MessageNotificationId notificationId = nextMessageNotificationId();
                markAwaitingValidation(gossipMessage, notificationId, registeredClients.size() - 1);

                var notification = new APIPacketGossipNotification(notificationId, dataType, data);
                for (var connection : registeredClients) {
                    if (connection.equals(originator)) {
                        continue;
                    }

                    connection.sendPacket(notification);
                }
                logger.debug("[{}] Message from API connected module sent to {} locally connected modules for validation!",
                        originator.name(), registeredClients.size() - 1);

                return;
            }
        } finally {
            registrationLock.readLock().unlock();
        }

        // there were no other LOCAL clients registered to this data type => spread it in the network
        spreadDataIntoNetwork(gossipMessage);
    }

    public void handleIncomingKnowledgeSpread(EstablishedSession session, GossipPacketSpreadKnowledge packet) {
        // TODO session-based rate limit!

        AtomicBoolean didExist = new AtomicBoolean(true);
        var gossipMessage = gossipKnowledgeBase.get(packet.messageId, (id) -> {
            // we use this supplier based creation to be thread safe. The add method of the `Cache` class
            // as no way to determine if a given record was already present!
            didExist.set(false);

            // 0 signals unlimited hops
            int nextTTL = packet.ttl != 1
                    ? Math.max(0, packet.ttl - 1) // either it stays 0 or is decremented by one
                    : -1; // -1 signals, that packet reached end of life at this hop
            return new GossipMessage(packet.messageId, session, nextTTL, packet.dataType, packet.data);
        });

        if (didExist.get()) {
            // we already sent it, or we already received this packet!
            // it is important to ignore anything else, to counter message injection/override attacks
            return;
        }

        registrationLock.readLock().lock();
        try {
            Set<APIConnection> connections = messageNotificationRegistrations.get(packet.dataType);
            if (connections == null) {
                return; // no registrations for this data type
            }

            // we continue to hold the lock, as we iterate over the `connections` set below!

            MessageNotificationId notificationId = nextMessageNotificationId();

            logger.debug("[{}] Message {} from network sent to {} locally connected modules for validation!",
                    session, gossipMessage.messageId, connections.size());

            // if ttl hasn't expired, queue it for spreading further into the network (mark it to await validations)
            if (gossipMessage.shouldForward()) {
                // we only need the translation if we await for validation!
                markAwaitingValidation(gossipMessage, notificationId, connections.size());

                // TODO maybe still record all validations (maybe we need to send all data to new incoming API connections?)
            }

            var notification = new APIPacketGossipNotification(notificationId, packet.dataType, packet.data);
            for (var connection : connections) {
                connection.sendPacket(notification);
            }
        } finally {
            registrationLock.readLock().unlock();
        }
    }

    public void receiveMessageValidation(MessageNotificationId notificationId, boolean valid) throws GossipException {
        var messageId = messageIdTranslation.get(notificationId);
        if (messageId == null) {
            // we didn't expect validation packets for this message!
            logger.debug("[{}] Message id translation not found! Potentially not expecting validation!", notificationId);
            throw new GossipException(GossipException.Type.INVALID_MESSAGE_ID);
        }

        if (!valid) {
            gossipKnowledgeBase.invalidate(messageId);
            return;
        }

        var gossipMessage = gossipKnowledgeBase.getIfPresent(messageId);
        if (gossipMessage == null) {
            // either ttl of the packet ran out (was never in the cache)
            // or cache entry was evicted (due to space constraints or a connection reporting invalid data).
            logger.debug("[{}] Message not found in our knowledge base! Maybe not expecting to forward!", messageId);
            throw new GossipException(GossipException.Type.INVALID_MESSAGE_ID);
        }

        boolean continueSending = gossipMessage.reportValidity();
        if (!continueSending) {
            return; // not all connections did report validity status yet. We continue to wait!
        }

        logger.debug("[{}] Message was validated by all local modules! Adopting knowledge now!", messageId);

        // continue to spread the data into the network!
        spreadDataIntoNetwork(gossipMessage);
    }

    private void spreadDataIntoNetwork(GossipMessage gossipMessage) {
        var packet = new GossipPacketSpreadKnowledge(gossipMessage.messageId, gossipMessage.nextTTL, gossipMessage.dataType, gossipMessage.data);

        int count = 0;
        sessionListLock.readLock().lock();
        try {
            // TODO be more lenient on the network (spread at random, in intervals!)
            for (EstablishedSession session : sessionList) {
                boolean didChange = gossipMessage.completedTransmissions.add(session);
                if (!didChange) {
                    continue; // if set didn't change, it was already on the list.
                }

                count += 1;

                session.sendPacket(packet);
            }
        } finally {
            sessionListLock.readLock().unlock();
        }

        logger.debug("[{}] Knowledge spread to {} peers!", gossipMessage.messageId, count);
    }

    private MessageNotificationId nextMessageNotificationId() {
        MessageNotificationId id = MessageNotificationId.createEmpty();
        do {
            ThreadLocalRandom.current().nextBytes(id.messageId());
        } while (messageIdTranslation.containsKey(id));
        return id;
    }

    private GossipMessage allocateGossipMessage(int ttl, DataType dataType, byte[] data) {
        GossipMessageId id = GossipMessageId.createEmpty();
        GossipMessage message;

        AtomicBoolean created = new AtomicBoolean();

        do {
            ThreadLocalRandom.current().nextBytes(id.messageId());

            // we can't guarantee it to be globally unique, but at least we don't try our best to avoid local conflicts.
            message = gossipKnowledgeBase.get(id, messageId -> {
                created.set(true);
                return new GossipMessage(messageId, ttl, dataType, data);
            });
        } while (!created.get());

        return message;
    }

    private void markAwaitingValidation(GossipMessage message, MessageNotificationId notificationId, int pendingValidations) {
        message.initPendingValidations(notificationId, pendingValidations);
        messageIdTranslation.put(notificationId, message.messageId); // we only need the translation if we await for validation!
    }

    public ProtocolDescription newServerProtocol() {
        Preconditions.checkNotNull(hostKey);

        HostKeyCertificate certificate = new HostKeySelfSignedX509Certificates(hostKey);

        return PROTOCOL
                .clone()
                .withSslContext(
                        SslContextBuilder.forServer(certificate.privateKey(), certificate.certificates())
                                .clientAuth(ClientAuth.REQUIRE)
                                .trustManager(new SelfSignedCertifyingTrustManager(certificate.getClass()))
                                .protocols(certificate.tlsVersionString())
                                .ciphers(certificate.tlsCipherSuites())
                );
    }

    public ProtocolDescription newClientProtocol(GossipPeerInfo remotePeerInfo) {
        Preconditions.checkNotNull(hostKey);

        HostKeyCertificate certificate = new HostKeySelfSignedX509Certificates(hostKey);

        return PROTOCOL
                .clone()
                .withSslContext(
                        SslContextBuilder.forClient()
                                .keyManager(certificate.privateKey(), certificate.certificates())
                                .trustManager(new SelfSignedCertifyingTrustManager(certificate.getClass(), remotePeerInfo))
                                .protocols(certificate.tlsVersionString())
                                .ciphers(certificate.tlsCipherSuites())
                );
    }
}