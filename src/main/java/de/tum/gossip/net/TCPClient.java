package de.tum.gossip.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

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

    public TCPClient(String hostname, int port, EventLoopGroup eventLoopGroup, ProtocolDescription protocol, Supplier<? extends InboundPacketHandler> defaultHandlerSupplier) {
        this.hostname = hostname;
        this.port = port;
        this.eventLoopGroup = eventLoopGroup;
        this.protocol = protocol;
        this.defaultHandlerSupplier = defaultHandlerSupplier;
    }

    public void connect() {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap
                    .group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ConnectionInitializer(protocol, defaultHandlerSupplier));

            channel = bootstrap
                    .connect(hostname, port)
                    .sync() // TODO do we want to sync this?
                    .channel();
        } catch (Exception e) {
            System.out.println("Failed to start client:");
            e.printStackTrace();
            // TODO stop the eventLoop sometime! and notify caller about error!
        }
    }

    public ChannelFuture disconnect() {
        if (channel == null) {
            throw new RuntimeException("Cannot disconnect client which isn't connected!");
        }

        var future = channel.close();
        channel = null;
        return future;
    }
}