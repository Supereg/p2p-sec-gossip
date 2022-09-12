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
    public final String hostname;
    public final int port;
    private final EventLoopGroup eventLoopGroup;

    private final ProtocolDescription protocol;
    private final Supplier<? extends InboundPacketHandler> defaultHandlerSupplier;

    private Channel channel;
    private ChannelState state = ChannelState.FREE;

    /**
     * Promise infrastructure provided by the common net layer. An implementation must ensure this promise
     * is marked as succeeded or failed accordingly if API consumers rely on this promise to retrieve connection state.
     */
    private Promise<ChannelInboundHandler> handshakePromise;

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

        state = ChannelState.CONNECTING;

        if (this.handshakePromise.isDone()) {
            // on multiple reconnects we need to create a new handshake promise
            this.handshakePromise = eventLoopGroup.next().newPromise();
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
                            handleClosed(null);
                        }
                    }
                });
    }

    public Future<ChannelInboundHandler> handshakeFuture() {
        return handshakePromise;
    }

    public ChannelState state() {
        return state;
    }

    /**
     * @return Returns the close future when disconnecting. When not in FREE state, can be cast to {@link ChannelFuture}.
     */
    public synchronized Future<Void> disconnect() {
        if (state == ChannelState.FREE) {
            return eventLoopGroup.next().newSucceededFuture(null);
        }

        if (state != ChannelState.CONNECTED) {
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

        if (future == null || !future.isDone()) {
            handshakePromise.setFailure(new Exception("Failed to connected or disconnected!"));
        }
    }

    @Override
    public String toString() {
        return hostname + ":" + port;
    }
}