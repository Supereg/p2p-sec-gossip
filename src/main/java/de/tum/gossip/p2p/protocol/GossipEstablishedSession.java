package de.tum.gossip.p2p.protocol;

import com.google.common.base.Preconditions;
import de.tum.gossip.api.packets.GossipAPIPacket;
import de.tum.gossip.net.ChannelInboundHandler;
import de.tum.gossip.net.ConnectionInitializer;
import de.tum.gossip.net.packets.OutboundPacket;
import de.tum.gossip.net.util.ChannelCloseReason;
import de.tum.gossip.net.util.ChannelCloseReasonCause;
import de.tum.gossip.p2p.GossipModule;
import de.tum.gossip.p2p.GossipPeerInfo;
import de.tum.gossip.p2p.packets.GossipHandshakeComplete;
import de.tum.gossip.p2p.packets.GossipPacketDisconnect;
import de.tum.gossip.p2p.packets.GossipPacketSpreadKnowledge;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by Andi on 07.07.22.
 */
public class GossipEstablishedSession implements GossipPacketHandler, EstablishedSession {
    private final Logger logger = LogManager.getLogger(GossipEstablishedSession.class);
    private final GossipModule module;
    private final GossipPeerInfo remotePeerInfo;
    /**
     * If this is {@code ture}, this session was established at the server side by an incoming connection from a remote peer.
     */
    private final boolean serverBound;

    private ChannelInboundHandler channel;

    public GossipEstablishedSession(GossipModule module, GossipPeerInfo remotePeerInfo, boolean serverBound) {
        this.module = module;
        this.remotePeerInfo = remotePeerInfo;
        this.serverBound = serverBound;
    }

    @Override
    public String name() {
        return channel.getHandle().remoteAddress().toString();
    }

    @Override
    public Logger logger() {
        return logger;
    }

    public GossipPeerInfo remotePeerInfo() {
        return remotePeerInfo;
    }

    @Override
    public ChannelInboundHandler channel() {
        return channel;
    }

    @Override
    public void onConnect(ChannelInboundHandler channel) {
        this.channel = channel;

        var result = module.adoptSession(this);
        if (result.isPresent()) {
            this.channel.close(result.get());
            return;
        }

        if (serverBound) {
            // if we are server bound we must send the final handshake complete message!
            channel.sendPacket(new GossipHandshakeComplete());
        }

        // handshake and session establishment is considered successful from this point onwards
        logger.info("Completed Handshake with client from {}", channel.getHandle().remoteAddress());
        channel.handshakePromise().setSuccess(channel);
    }

    @Override
    public void onHandlerRemove() {
        if (channel != null) {
            channel.getHandle().pipeline().remove(ConnectionInitializer.Ids.TIMEOUT);
        }
    }

    @Override
    public void onDisconnect(ChannelCloseReason reason) {
        GossipPacketDisconnect.Reason.of(reason)
                .ifPresent(value -> channel.sendPacket(new GossipPacketDisconnect(value)));

        var promise = channel.handshakePromise();
        if (!promise.isDone()) {
            promise.setFailure(new ChannelCloseReasonCause(reason));
        }

        module.handleSessionDisconnect(this);
    }

    public void handle(GossipPacketSpreadKnowledge packet) {
        module.handleIncomingKnowledgeSpread(this, packet);
    }

    @Override
    public void handle(GossipPacketDisconnect packet) {
        channel.close(packet.channelCloseReason(channel));
    }

    @Override
    public <Packet extends OutboundPacket> void sendPacket(Packet packet, GenericFutureListener<? extends Future<? super Void>>[] genericFutureListeners) {
        Preconditions.checkNotNull(channel);
        Preconditions.checkState(!(packet instanceof GossipAPIPacket));
        channel.sendPacket(packet, genericFutureListeners);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GossipEstablishedSession that = (GossipEstablishedSession) o;
        return remotePeerInfo().equals(that.remotePeerInfo());
    }

    @Override
    public int hashCode() {
        return remotePeerInfo().hashCode();
    }

    public GossipPeerInfo peerInfo() {
        return remotePeerInfo;
    }

    @Override
    public boolean isServerBound() {
        return serverBound;
    }
}