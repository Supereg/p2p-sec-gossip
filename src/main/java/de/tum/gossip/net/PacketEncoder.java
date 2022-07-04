package de.tum.gossip.net;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.Optional;

/**
 * Created by Andi on 21.06.22.
 */
public class PacketEncoder extends MessageToByteEncoder<OutboundPacket> {
    @Override
    protected void encode(ChannelHandlerContext ctx, OutboundPacket msg, ByteBuf out) throws Exception {
        ProtocolDescription protocol = ctx.channel().attr(ChannelInboundHandler.PROTOCOL_DESCRIPTION_KEY).get();

        Preconditions.checkState(protocol != null, "ProtocolDescription wasn't set for channel!");

        Optional<Integer> packetId = protocol.packetIdFromPacket(msg);
        if (packetId.isEmpty()) {
            throw new Exception("Tried to encode unregistered message type for packet: " + msg);
        }

        out.writeShort(packetId.get());
        try {
            msg.serialize(out);
        } catch (Exception e) {
            throw new Exception("Error writing packet contents of type " + msg.getClass().getSimpleName() + " to buffer", e);
        }
    }
}