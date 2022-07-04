package de.tum.gossip.api;

import de.tum.gossip.net.OutboundPacket;
import io.netty.buffer.ByteBuf;

/**
 * Created by Andi on 21.06.22.
 */
public class GossipNotificationPacket implements OutboundPacket {
    int messageId;
    int dataType;
    byte[] data;

    public GossipNotificationPacket() {}

    public GossipNotificationPacket(int messageId, int dataType, byte[] data) {
        this.messageId = messageId;
        this.dataType = dataType;
        this.data = data;
    }

    // TODO do we need really setter methods? or should we set all the attributes in
    // constructor method and remove these?
    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public void serialize(ByteBuf byteBuf) {
        byteBuf.writeShort(messageId);
        byteBuf.writeShort(dataType);
        byteBuf.writeBytes(data);
    }
}