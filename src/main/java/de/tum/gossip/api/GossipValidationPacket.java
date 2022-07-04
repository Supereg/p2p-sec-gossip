package de.tum.gossip.api;

import de.tum.gossip.net.InboundPacket;
import io.netty.buffer.ByteBuf;

/**
 * Created by Andi on 21.06.22.
 */
public class GossipValidationPacket implements InboundPacket<GossipAPIPacketHandler> {
    int messageId;
    boolean v;

    public GossipValidationPacket() {}

    public int getMessageId() {
        return messageId;
    }

    public boolean isValid() {
        return v;
    }

    @Override
    public void deserialize(ByteBuf byteBuf) {
        messageId = byteBuf.readShort();
        v = byteBuf.readShort() % 2 == 1;
    }

    @Override
    public void accept(GossipAPIPacketHandler handler) {
        handler.handle(this);
    }
}