package de.tum.gossip.p2p.protocol;

import de.tum.gossip.crypto.GossipCrypto;
import de.tum.gossip.net.util.ChannelCloseReason;
import de.tum.gossip.net.util.ChannelCloseReasonCause;
import de.tum.gossip.net.ChannelInboundHandler;
import de.tum.gossip.net.ConnectionInitializer;
import de.tum.gossip.p2p.GossipModule;
import de.tum.gossip.p2p.GossipPeerInfo;
import de.tum.gossip.p2p.packets.*;
import de.tum.gossip.p2p.packets.GossipPacketDisconnect.Reason;
import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by Andi on 06.07.22.
 */
public class GossipClientHandshakeListener implements GossipPacketHandler {
    private final Logger logger = LogManager.getLogger(GossipClientHandshakeListener.class);
    private final GossipModule gossipModule;
    private final GossipPeerInfo serverPeerInfo;

    private ChannelInboundHandler channel;

    public GossipClientHandshakeListener(GossipModule gossipModule, GossipPeerInfo serverPeerInfo) {
        this.gossipModule = gossipModule;
        this.serverPeerInfo = serverPeerInfo;
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
        // we wait maximum 10 seconds for a response from the remote.
        channel.getHandle().pipeline().addFirst(ConnectionInitializer.Ids.TIMEOUT, new ReadTimeoutHandler(10));

        this.channel = channel;

        // we need to wait till the TLS handshake completes.
        // after that we can get access to the TLS certificate to verify that it was signed by the server's identity.
        SslHandler handler = GossipCrypto.getSslHandler(channel);
        handler.handshakeFuture()
                .addListener(this::onTLSHandshakeComplete);
    }

    private synchronized void onTLSHandshakeComplete(Future<? super Channel> future) {
        if (this.channel == null || !this.channel.isConnected()) {
            return;
        }

        if (!future.isSuccess()) {
            this.channel.close(new ChannelCloseReason.Exception("Failed to perform TLS handshake: " + future.cause().getMessage(), future.cause()));
            return;
        }

        // At this point the full identity was verified through the `SelfSignedCertifyingTrustManager`.
        // We know the servers identity from the start, when we constructed the client.

        channel.sendPacket(new GossipPacketHandshakeHello());
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

    public synchronized void handle(GossipHandshakeComplete packet) {
        var handler = new GossipEstablishedSession(gossipModule, serverPeerInfo, false);
        channel.replacePacketHandler(handler);
    }

    @Override
    public void handle(GossipPacketDisconnect packet) {
        channel.close(packet.channelCloseReason(channel));
    }
}