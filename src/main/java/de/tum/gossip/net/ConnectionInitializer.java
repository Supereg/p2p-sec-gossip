package de.tum.gossip.net;

import de.tum.gossip.net.packets.InboundPacketHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.Promise;

import java.nio.ByteOrder;
import java.util.function.Supplier;

/**
 * Created by Andi on 21.06.22.
 */
public class ConnectionInitializer extends ChannelInitializer<Channel> {
    private final ProtocolDescription protocol;
    private final Supplier<? extends InboundPacketHandler> defaultHandlerSupplier;
    private Promise<ChannelInboundHandler> handshakePromise;
    private final boolean registerInboundHandler;

    public static class Ids {
        public static final String TLS_HANDLER = "tlsHandler";
        public static final String FRAME_DECODER = "frameDecoder";
        public static final String DECODER = "decoder";
        public static final String FRAME_ENCODER = "frameEncoder";
        public static final String ENCODER = "encoder";
        public static final String TIMEOUT = "timeout";
    }

    private final SslContext sslContext;

    public <Handler extends InboundPacketHandler> ConnectionInitializer(
            ProtocolDescription protocol,
            Supplier<Handler> defaultHandlerSupplier,
            Promise<ChannelInboundHandler> handshakePromise
    ) {
        this(protocol, defaultHandlerSupplier, handshakePromise, true);
    }

    public <Handler extends InboundPacketHandler> ConnectionInitializer(
            ProtocolDescription protocol,
            Supplier<Handler> defaultHandlerSupplier,
            Promise<ChannelInboundHandler> handshakePromise,
            boolean registerInboundHandler
    ) {
        this.protocol = protocol;
        this.defaultHandlerSupplier = defaultHandlerSupplier;
        this.handshakePromise = handshakePromise;
        this.registerInboundHandler = registerInboundHandler;

        this.sslContext = protocol.getSslContext();
    }

    @Override
    public void initChannel(Channel channel) {
        channel.attr(ChannelInboundHandler.PROTOCOL_DESCRIPTION_KEY).set(protocol);

        if (handshakePromise == null) {
            handshakePromise = channel.eventLoop().newPromise();
        }

        ChannelPipeline pipeline = channel.pipeline();

        InboundPacketHandler defaultHandler = this.defaultHandlerSupplier.get();

        if (sslContext != null) {
            pipeline.addLast(Ids.TLS_HANDLER, sslContext.newHandler(channel.alloc()));
        }

        // inbound channel pipeline
        pipeline.addLast(Ids.FRAME_DECODER, new LengthFieldBasedFrameDecoder(
                ByteOrder.BIG_ENDIAN, // API messages use BIG endian representation
                65535, // API messages have a max size of UINT8_MAX
                0, // size field is located at offset 0
                2, // size field consists of two bytes
                -2, // the size field is included in the length calculation, therefore we subtract it.
                2, // amount of bytes stripped when forwarding message to the next handler! We remove the size field!
                true
        ));
        pipeline.addLast(Ids.DECODER, new PacketDecoder());

        // outbound channel pipeline
        pipeline.addLast(Ids.FRAME_ENCODER, new LengthFieldPrepender(
                ByteOrder.BIG_ENDIAN,
                2, // length of the length field
                0, // zero length adjustments
                true // size includes the length field itself!
        ));
        pipeline.addLast(Ids.ENCODER, new PacketEncoder());

        if (this.registerInboundHandler) {
            pipeline.addLast(new ChannelInboundHandler(defaultHandler, handshakePromise));
        }
    }
}