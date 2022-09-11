package de.tum.gossip.api.packets;

import de.tum.gossip.api.GossipAPIPacketHandler;
import de.tum.gossip.net.packets.InboundPacket;
import de.tum.gossip.p2p.util.MessageNotificationId;
import io.netty.buffer.ByteBuf;

/**
 * Created by Andi on 21.06.22.
 */
public class APIPacketGossipValidation implements GossipAPIPacket, InboundPacket<GossipAPIPacketHandler> {
    public MessageNotificationId messageId;
    public boolean valid;

    public APIPacketGossipValidation() {}

    @Override
    public void deserialize(ByteBuf byteBuf) {
        messageId = MessageNotificationId.createEmpty();
        byteBuf.readBytes(messageId.messageId());
        valid = (byteBuf.readUnsignedShort() & 0x01) == 1;
    }

    @Override
    public void accept(GossipAPIPacketHandler handler) {
        handler.handle(this);
    }
}