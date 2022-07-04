package de.tum.gossip.net;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

/**
 * Created by Andi on 21.06.22.
 */
public class PacketDecoder extends MessageToMessageDecoder<ByteBuf> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        ProtocolDescription protocol = ctx.channel().attr(ChannelInboundHandler.PROTOCOL_DESCRIPTION_KEY).get();

        Preconditions.checkState(protocol != null, "ProtocolDescription wasn't set for channel!");

        if (msg.readableBytes() < 2) {
            throw new Exception("Incoming packet has not enough bytes to hold the packet type!");
        }

        int packetId = msg.readUnsignedShort();
        var packet = protocol.newPacketInstanceFromInbound(packetId);
        if (packet.isEmpty()) {
            throw new Exception("Received unknown packet type: " + packetId);
        }

        packet.get().deserialize(msg);
        if (msg.readableBytes() > 0) {
            throw new Exception("Incoming packet of type " + packet.get().getClass().getSimpleName() + " is not exhausted. Found additional " + msg.readableBytes() + " bytes!");
        }

        out.add(packet.get());
    }
}