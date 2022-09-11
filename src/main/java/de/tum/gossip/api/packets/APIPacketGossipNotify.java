package de.tum.gossip.api.packets;

import de.tum.gossip.api.GossipAPIPacketHandler;
import de.tum.gossip.net.packets.InboundPacket;
import io.netty.buffer.ByteBuf;

/**
 * Created by Andi on 21.06.22.
 */
public class APIPacketGossipNotify implements GossipAPIPacket, InboundPacket<GossipAPIPacketHandler> {
    public int dataType;

    public APIPacketGossipNotify() {}

    @Override
    public void deserialize(ByteBuf byteBuf) {
        byteBuf.readByte(); // reserved
        byteBuf.readByte(); // reserved
        dataType = byteBuf.readUnsignedShort();
    }

    @Override
    public void accept(GossipAPIPacketHandler handler) {
        handler.handle(this);
    }
}