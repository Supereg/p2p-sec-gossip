package de.tum.gossip.p2p.protocol;

import de.tum.gossip.crypto.GossipCrypto;
import de.tum.gossip.crypto.PeerCertificateInfo;
import de.tum.gossip.crypto.PeerIdentity;
import de.tum.gossip.net.ChannelInboundHandler;
import de.tum.gossip.net.ConnectionInitializer;
import de.tum.gossip.net.util.ChannelCloseReason;
import de.tum.gossip.net.util.ChannelCloseReasonCause;
import de.tum.gossip.p2p.GossipModule;
import de.tum.gossip.p2p.GossipPeerInfo;
import de.tum.gossip.p2p.packets.GossipPacketDisconnect;
import de.tum.gossip.p2p.packets.GossipPacketDisconnect.Reason;
import de.tum.gossip.p2p.packets.GossipPacketHandshakeHello;
import de.tum.gossip.p2p.storage.StoredIdentity;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

/**
 * Created by Andi on 05.07.22.
 */
public class GossipServerHandshakeListener implements GossipPacketHandler {
    private final Logger logger = LogManager.getLogger(GossipServerHandshakeListener.class);
    private final GossipModule gossipModule;

    private ChannelInboundHandler channel;

    // below fields are populated once the handshake proceeds
    private GossipPeerInfo clientPeerInfo; // populated once `GossipPacketHandshakeHello` is received

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
        StoredIdentity storedIdentity = gossipModule.identityStorage.retrieveKey(identity);
        if (storedIdentity == null) {
            channel.close(new GossipPacketDisconnect.OutboundCloseReason(Reason.CANCELLED, "Couldn't find requesting peer's identity in local identity storage!"));
            return;
        }

        if (!Arrays.equals(tlsKey.getEncoded(), storedIdentity.publicKey().getEncoded())) { // not really necessary, but still desirable to have contracts explicit!
            channel.close(new GossipPacketDisconnect.OutboundCloseReason(Reason.AUTHENTICATION, "Expected keys to match!"));
            return;
        }

        // ensure that the TLS channel was established with a certificate which is associated with the peer's identity!
        try {
            peerCertificateInfo.hostKeyCertificate().verify(storedIdentity.publicKey(), BouncyCastleProvider.PROVIDER_NAME);
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException e) {
            // unlikely to happen, something went completely wrong!
            channel.close(new GossipPacketDisconnect.OutboundCloseReason(Reason.UNEXPECTED_FAILURE, e));
            return;
        } catch (SignatureException e) {
            // failed to verify signature!
            channel.close(new GossipPacketDisconnect.OutboundCloseReason(Reason.AUTHENTICATION, e));
            return;
        }

        clientPeerInfo = new GossipPeerInfo(identity, storedIdentity.publicKey());

        var remoteAddress = ((InetSocketAddress) channel.getHandle().remoteAddress()).getAddress().getHostAddress();
        if (!remoteAddress.equals(storedIdentity.lastSeenHostname())) {
            // We strictly bind the ip address to the peer's identity.
            // A future version of gossip could employ a PoW based procedure to update the host address at this point,
            // to allow for a more robust implementation.
            channel.close(new GossipPacketDisconnect.OutboundCloseReason(
                    Reason.AUTHENTICATION,
                    "Unknown host address (" + remoteAddress + " vs. expected " + storedIdentity.lastSeenHostname() + ")!"
            ));
            return;
        }

        // switching protocol state into SESSION
        var handler = new GossipEstablishedSession(gossipModule, clientPeerInfo, true);
        channel.replacePacketHandler(handler);
    }

    @Override
    public synchronized void handle(GossipPacketDisconnect packet) {
        channel.close(packet.channelCloseReason(channel));
    }
}