package de.tum.gossip.net;

import com.google.common.base.Preconditions;
import de.tum.gossip.net.packets.InboundPacketHandler;
import de.tum.gossip.net.util.ChannelState;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.function.Supplier;

/**
 * Created by Andi on 27.06.22.
 */
public class TCPClient {
    private final String hostname;
    private final int port;
    private final EventLoopGroup eventLoopGroup;

    private final ProtocolDescription protocol;
    private final Supplier<? extends InboundPacketHandler> defaultHandlerSupplier;

    private Channel channel;
    private ChannelState state = ChannelState.FREE;

    /**
     * Promise infrastructure provided by the common net layer. An implementation must ensure this promise
     * is marked as succeeded or failed accordingly if API consumers rely on this promise to retrieve connection state.
     */
    private final Promise<ChannelInboundHandler> handshakePromise;

    public TCPClient(
            String hostname,
            int port,
            EventLoopGroup eventLoopGroup,
            ProtocolDescription protocol,
            Supplier<? extends InboundPacketHandler> defaultHandlerSupplier
    ) {
        this.hostname = hostname;
        this.port = port;
        this.eventLoopGroup = eventLoopGroup;
        this.protocol = protocol;
        this.defaultHandlerSupplier = defaultHandlerSupplier;

        this.handshakePromise = eventLoopGroup.next().newPromise();
    }

    public synchronized ChannelFuture connect() {
        if (state != ChannelState.FREE) {
            throw new RuntimeException("Cannot connect client when in state " + state);
        }

        Bootstrap bootstrap = new Bootstrap();
        bootstrap
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ConnectionInitializer(protocol, defaultHandlerSupplier, handshakePromise));

        return bootstrap.connect(hostname, port)
                .addListener(future -> {
                    synchronized (this) {
                        if (future.isSuccess()) {
                            channel = ((ChannelFuture) future).channel();
                            state = ChannelState.CONNECTED;

                            channel.closeFuture().addListener(this::handleClosed);
                        } else {
                            state = ChannelState.FREE;
                        }
                    }
                });
    }

    public Future<de.tum.gossip.net.ChannelInboundHandler> handshakeFuture() {
        return handshakePromise;
    }

    public synchronized ChannelFuture disconnect() {
        if (state  != ChannelState.CONNECTED) {
            throw new RuntimeException("Cannot disconnect client which is in state " + state);
        }

        Preconditions.checkNotNull(channel);
        state = ChannelState.DISCONNECTING;

        // we already subscribed the close future above!
        return channel.close();
    }

    private synchronized <F extends Future<? super Void>> void handleClosed(F future) {
        channel = null;
        state = ChannelState.FREE;
    }
}