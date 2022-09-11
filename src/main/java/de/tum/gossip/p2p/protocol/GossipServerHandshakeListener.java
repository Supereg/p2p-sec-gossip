package de.tum.gossip.p2p.protocol;

import de.tum.gossip.crypto.GossipCrypto;
import de.tum.gossip.crypto.PeerCertificateInfo;
import de.tum.gossip.crypto.PeerIdentity;
import de.tum.gossip.net.util.ChannelCloseReason;
import de.tum.gossip.net.util.ChannelCloseReasonCause;
import de.tum.gossip.net.ChannelInboundHandler;
import de.tum.gossip.net.ConnectionInitializer;
import de.tum.gossip.p2p.GossipModule;
import de.tum.gossip.p2p.GossipPeerInfo;
import de.tum.gossip.p2p.packets.GossipHandshakeComplete;
import de.tum.gossip.p2p.packets.GossipPacketDisconnect;
import de.tum.gossip.p2p.packets.GossipPacketDisconnect.Reason;
import de.tum.gossip.p2p.packets.GossipPacketHandshakeHello;
import de.tum.gossip.p2p.packets.GossipPacketHandshakeIdentityVerification2;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

/**
 * Created by Andi on 05.07.22.
 * TODO docs: handshake listener on the server side, handling an incoming client!
 */
public class GossipServerHandshakeListener implements GossipPacketHandler {
    private final Logger logger = LogManager.getLogger(GossipServerHandshakeListener.class);
    private final GossipModule gossipModule;

    private ChannelInboundHandler channel;

    // below fields are populated once the handshake proceeds
    private GossipPeerInfo clientPeerInfo; // populated once `GossipPacketHandshakeHello` is received
    private final byte[] clientChallenge = new byte[8]; // 64 bits for the challenge response

    public GossipServerHandshakeListener(GossipModule gossipModule) {
        this.gossipModule = gossipModule;
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public ChannelInboundHandler channel() {
        return channel;
    }

    @Override
    public synchronized void onConnect(ChannelInboundHandler channel) {
        // our protocol requires the client to "make the first move".
        // we require that this happens within 10 seconds, otherwise we will disconnect the client!
        channel.getHandle().pipeline().addFirst(ConnectionInitializer.Ids.TIMEOUT, new ReadTimeoutHandler(10));

        this.channel = channel;
    }

    @Override
    public synchronized void onHandlerRemove() {
        if (channel != null) {
            channel.getHandle().pipeline().remove(ConnectionInitializer.Ids.TIMEOUT);
        }
    }

    @Override
    public synchronized void onDisconnect(ChannelCloseReason reason) {
        Reason.of(reason)
                .ifPresent(value -> channel.sendPacket(new GossipPacketDisconnect(value)));

        this.channel.handshakePromise().setFailure(new ChannelCloseReasonCause(reason));
        this.channel = null;
        // TODO any notifications to do?
    }

    public synchronized void handle(GossipPacketHandshakeHello packet) {
        if (clientPeerInfo != null) {
            channel.close(new GossipPacketDisconnect.OutboundCloseReason(Reason.CANCELLED, "Received HandshakeHello in illegal state!"));
            return;
        }

        PeerCertificateInfo peerCertificateInfo;
        try {
            peerCertificateInfo = GossipCrypto.peerCertificateFromTLSSession(channel);
        } catch (SSLPeerUnverifiedException | IllegalStateException e) {
            channel.close(new GossipPacketDisconnect.OutboundCloseReason(Reason.UNEXPECTED_FAILURE, e));
            return;
        }

        if (packet.version != GossipPacketHandshakeHello.Version.CURRENT) {
            channel.close(new GossipPacketDisconnect.OutboundCloseReason(Reason.UNSUPPORTED, new GossipPacketHandshakeHello.UnsupportedVersionException()));
            return;
        }

        if (!(peerCertificateInfo.hostKeyCertificate().getPublicKey() instanceof RSAPublicKey tlsKey)) {
            channel.close(new GossipPacketDisconnect.OutboundCloseReason(Reason.AUTHENTICATION, "Unexpected certificate chain. Root is not a host key!"));
            return;
        }

        PeerIdentity identity = new PeerIdentity(tlsKey);

        // public part of the host key of the remote peer's identity.
        RSAPublicKey publicKey = gossipModule.identityStorage.retrieveKey(identity);
        if (publicKey == null) {
            // TODO implement ability to "register", => via trust on first use?
            channel.close(new GossipPacketDisconnect.OutboundCloseReason(Reason.CANCELLED, "Couldn't find requesting peer's identity in local identity storage!"));
            return;
        }

        if (!Arrays.equals(tlsKey.getEncoded(), publicKey.getEncoded())) { // not really necessary, but still desirable to have contracts explicit!
            channel.close(new GossipPacketDisconnect.OutboundCloseReason(Reason.AUTHENTICATION, "Expected keys to match!"));
            return;
        }

        // ensure that the TLS channel was established with a certificate which is associated with the peer's identity!
        try {
            peerCertificateInfo.hostKeyCertificate().verify(publicKey, BouncyCastleProvider.PROVIDER_NAME);
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException e) {
            // unlikely to happen, something went completely wrong!
            channel.close(new GossipPacketDisconnect.OutboundCloseReason(Reason.UNEXPECTED_FAILURE, e));
            return;
        } catch (SignatureException e) {
            // failed to verify signature!
            channel.close(new GossipPacketDisconnect.OutboundCloseReason(Reason.AUTHENTICATION, e));
            return;
        }

        clientPeerInfo = new GossipPeerInfo(identity, publicKey);

        var signature = GossipCrypto.Signature.signChallenge(gossipModule.hostKey, packet.serverChallenge);
        GossipCrypto.SECURE_RANDOM.nextBytes(clientChallenge);

        // TODO channel.sendPacket(new GossipPacketHandshakeIdentityVerification1(signature, clientChallenge));

        // TODO PoW challenge, if we encounter an unexpected ip address!
        channel.sendPacket(new GossipHandshakeComplete());
        switchProtocols();
    }

    public synchronized void handle(GossipPacketHandshakeIdentityVerification2 packet) {
        if (clientPeerInfo == null) {
            channel.close(new GossipPacketDisconnect.OutboundCloseReason(Reason.CANCELLED, "Received PacketHandshakeIdentityVerification in illegal state!"));
            return;
        }

        // verify that the specified identity is actually owned by our remote peer right now
        if (!GossipCrypto.Signature.verifyChallenge(clientPeerInfo, packet.signature, clientChallenge)) {
            channel.close(new GossipPacketDisconnect.OutboundCloseReason(Reason.AUTHENTICATION, "Failed to verify signature of identity of remote peer!"));
            return;
        }

        channel.sendPacket(new GossipHandshakeComplete());
        switchProtocols();
    }

    private synchronized void switchProtocols() {
        // switching protocol state into SESSION
        var handler = new GossipEstablishedSession(gossipModule, clientPeerInfo);
        channel.replacePacketHandler(handler);
    }

    @Override
    public synchronized void handle(GossipPacketDisconnect packet) {
        channel.close(packet.channelCloseReason(channel));
    }
}