package de.tum.gossip.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Created by Andi on 21.06.22.
 */
public class TCPServer {
    private final String hostname;
    private final int port;
    private final EventLoopGroup eventLoopGroup;

    private final ProtocolDescription protocol;
    private final Supplier<? extends InboundPacketHandler> defaultHandlerSupplier;

    private Channel channel;

    TCPServer(@Nullable String hostname, int port, EventLoopGroup eventLoopGroup, ProtocolDescription protocol, Supplier<? extends InboundPacketHandler> defaultHandlerSupplier) {
        this.hostname = hostname;
        this.port = port;
        this.eventLoopGroup = eventLoopGroup;
        this.protocol = protocol;
        this.defaultHandlerSupplier = defaultHandlerSupplier;
    }

    public void run() {
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap
                    .group(eventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ConnectionInitializer(protocol, defaultHandlerSupplier));

            channel = (hostname != null && !hostname.isEmpty() ? bootstrap.bind(hostname, port) : bootstrap.bind(port))
                    .sync() // TODO ability to do this asnyc!
                    .channel();
        } catch (Exception e) {
            // TODO proper error logging
            System.out.println("Failed to start server:");
            e.printStackTrace();

            // TODO stop the eventLoop sometime! and notify caller about error!
        }
    }

    public ChannelFuture stop() {
        if (channel == null) {
            throw new RuntimeException("Cannot stop server which is already stopped!");
        }

        var future = channel.close();
        channel = null;
        return future;
    }
}