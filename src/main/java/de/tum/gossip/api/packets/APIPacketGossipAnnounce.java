package de.tum.gossip.api.packets;

import de.tum.gossip.api.GossipAPIPacketHandler;
import de.tum.gossip.net.packets.InboundPacket;
import io.netty.buffer.ByteBuf;

/**
 * Created by Andi on 21.06.22.
 */
public class APIPacketGossipAnnounce implements GossipAPIPacket, InboundPacket<GossipAPIPacketHandler> {
    public int ttl;
    public int dataType;
    public byte[] data;

    public APIPacketGossipAnnounce() {}

    @Override
    public void deserialize(ByteBuf byteBuf) {
        ttl = byteBuf.readUnsignedByte();
        byteBuf.readByte(); // reserved
        dataType = byteBuf.readUnsignedShort();
        data = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(data);
    }

    @Override
    public void accept(GossipAPIPacketHandler handler) {
        handler.handle(this);
    }
}