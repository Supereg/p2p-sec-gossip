package de.tum.gossip.net;

import com.google.common.base.Preconditions;
import de.tum.gossip.net.packets.InboundPacketHandler;
import de.tum.gossip.net.util.ChannelState;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Created by Andi on 21.06.22.
 */
public class TCPServer {
    public final String hostname;
    public final int port;
    private final EventLoopGroup eventLoopGroup;

    private final ProtocolDescription protocol;
    private final Supplier<? extends InboundPacketHandler> defaultHandlerSupplier;

    private Channel channel;
    private ChannelState state = ChannelState.FREE;

    TCPServer(
            @Nullable String hostname,
            int port,
            EventLoopGroup eventLoopGroup,
            ProtocolDescription protocol, Supplier<? extends InboundPacketHandler> defaultHandlerSupplier) {
        this.hostname = hostname;
        this.port = port;
        this.eventLoopGroup = eventLoopGroup;
        this.protocol = protocol;
        this.defaultHandlerSupplier = defaultHandlerSupplier;
    }

    public synchronized ChannelFuture bind() {
        if (state != ChannelState.FREE) {
            throw new RuntimeException("Cannot bind server when in state " + state);
        }

        state = ChannelState.CONNECTING;

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap
                .group(eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ConnectionInitializer(protocol, defaultHandlerSupplier, null));

        return (hostname != null && !hostname.isEmpty() ? bootstrap.bind(hostname, port) : bootstrap.bind(port))
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

    public ChannelState state() {
        return state;
    }

    /**
     * @return Returns the Close future when stopping. When not in FREE state, can be cast to {@link ChannelFuture}.
     */
    public synchronized Future<Void> stop() {
        if (state == ChannelState.FREE) {
            return eventLoopGroup.next().newSucceededFuture(null);
        }

        if (state != ChannelState.CONNECTED) {
            throw new RuntimeException("Cannot stop server which is in state " + state);
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