package de.tum.gossip.api.packets;

import de.tum.gossip.net.packets.OutboundPacket;
import de.tum.gossip.p2p.util.DataType;
import de.tum.gossip.p2p.util.MessageNotificationId;
import io.netty.buffer.ByteBuf;

/**
 * Created by Andi on 21.06.22.
 */
public class APIPacketGossipNotification implements GossipAPIPacket, OutboundPacket {
    public MessageNotificationId messageId;
    public DataType dataType;
    public byte[] data;

    public APIPacketGossipNotification() {}

    public APIPacketGossipNotification(MessageNotificationId messageId, DataType dataType, byte[] data) {
        this.messageId = messageId;
        this.dataType = dataType;
        this.data = data;
    }

    @Override
    public void serialize(ByteBuf byteBuf) {
        byteBuf.writeBytes(messageId.messageId());
        byteBuf.writeShort(dataType.dataType());
        byteBuf.writeBytes(data);
    }
}