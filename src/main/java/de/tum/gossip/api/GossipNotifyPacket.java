package de.tum.gossip.api;

import de.tum.gossip.net.InboundPacket;
import io.netty.buffer.ByteBuf;

/**
 * Created by Andi on 21.06.22.
 */
public class GossipNotifyPacket implements InboundPacket<GossipAPIPacketHandler> {
    int dataType;

    public GossipNotifyPacket() {}

    public int getDataType() {
        return dataType;
    }

    @Override
    public void deserialize(ByteBuf byteBuf) {
        byteBuf.readBytes(2); // reserved
        dataType = byteBuf.readShort();
    }

    @Override
    public void accept(GossipAPIPacketHandler handler) {
        handler.handle(this);
    }
}