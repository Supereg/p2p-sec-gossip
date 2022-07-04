package de.tum.gossip.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.nio.ByteOrder;
import java.util.function.Supplier;

/**
 * Created by Andi on 21.06.22.
 */
public class ConnectionInitializer extends ChannelInitializer<Channel> {
    private final ProtocolDescription protocol;
    private final Supplier<? extends InboundPacketHandler> defaultHandlerSupplier;
    private final boolean registerInboundHandler;

    public <Handler extends InboundPacketHandler> ConnectionInitializer(
            ProtocolDescription protocol,
            Supplier<Handler> defaultHandlerSupplier
    ) {
        this(protocol, defaultHandlerSupplier, true);
    }

    public <Handler extends InboundPacketHandler> ConnectionInitializer(
            ProtocolDescription protocol,
            Supplier<Handler> defaultHandlerSupplier,
            boolean registerInboundHandler
    ) {
        this.protocol = protocol;
        this.defaultHandlerSupplier = defaultHandlerSupplier;
        this.registerInboundHandler = registerInboundHandler;
    }

    @Override
    public void initChannel(Channel channel) throws Exception {
        channel.attr(ChannelInboundHandler.PROTOCOL_DESCRIPTION_KEY).set(protocol);

        ChannelPipeline pipeline = channel.pipeline();

        InboundPacketHandler defaultHandler = this.defaultHandlerSupplier.get();

        // inbound channel pipeline
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
                ByteOrder.BIG_ENDIAN, // API messages use BIG endian representation
                65535, // API messages have a max size of UINT8_MAX
                0, // size field is located at offset 0
                2, // size field consists of two bytes
                -2, // the size field is included in the length calculation, therefore we subtract it.
                2, // amount of bytes stripped when forwarding message to the next handler! We remove the size field!
                true
        ));
        pipeline.addLast("decoder", new PacketDecoder());

        // outbound channel pipeline
        pipeline.addLast("frameEncoder", new LengthFieldPrepender(
                ByteOrder.BIG_ENDIAN,
                2, // length of the length field
                0, // zero length adjustments
                true // size includes the length field itself!
        ));
        pipeline.addLast("encoder", new PacketEncoder());

        if (this.registerInboundHandler) {
            pipeline.addLast(new ChannelInboundHandler(protocol, defaultHandler));
        }
    }
}