package de.tum.gossip.api;

import de.tum.gossip.net.InboundPacket;
import io.netty.buffer.ByteBuf;

/**
 * Created by Andi on 21.06.22.
 */
public class GossipAnnouncePacket implements InboundPacket<GossipAPIPacketHandler> {
    int ttl;
    int dataType;
    byte[] data;

    public GossipAnnouncePacket() {}

    public int getTTL() {
        return ttl;
    }

    public int getDataType() {
        return dataType;
    }

    @Override
    public void deserialize(ByteBuf byteBuf) {
        ttl = byteBuf.readByte();
        byteBuf.readByte(); // reserved
        dataType = byteBuf.readShort();
        data = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(data);
    }

    @Override
    public void accept(GossipAPIPacketHandler handler) {
        handler.handle(this);
    }
}