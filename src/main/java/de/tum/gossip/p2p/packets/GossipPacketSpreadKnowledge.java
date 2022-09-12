package de.tum.gossip.p2p.packets;

import com.google.common.base.Preconditions;
import de.tum.gossip.net.packets.Packet;
import de.tum.gossip.p2p.protocol.GossipEstablishedSession;
import de.tum.gossip.p2p.util.DataType;
import de.tum.gossip.p2p.util.GossipMessageId;
import io.netty.buffer.ByteBuf;

/**
 * Created by Andi on 06.07.22.
 */
public class GossipPacketSpreadKnowledge implements Packet<GossipEstablishedSession> {
    /**
     * Random identifier for a routed packet. It is used to avoid cycles in packet routing.
     */
    public GossipMessageId messageId; // 8 bytes
    public int ttl; // 2 bytes
    public DataType dataType; // 2 bytes
    // There are 4 bytes of reserved within the header for future extensions and alignment reasons. Set to zero on transmission

    public byte[] data;

    public GossipPacketSpreadKnowledge() {}

    public GossipPacketSpreadKnowledge(GossipMessageId messageId, int ttl, DataType dataType, byte[] data) {
        Preconditions.checkState(messageId.messageId().length == 8);
        this.messageId = messageId;
        this.ttl = ttl;
        this.dataType = dataType;
        this.data = data;
    }

    @Override
    public void serialize(ByteBuf byteBuf) {
        Preconditions.checkState(messageId.messageId().length == 8);
        byteBuf.writeBytes(messageId.messageId());
        byteBuf.writeShort(ttl);
        byteBuf.writeShort(dataType.dataType());
        var reserved = new byte[4];
        byteBuf.writeBytes(reserved);

        byteBuf.writeBytes(data);
    }

    @Override
    public void deserialize(ByteBuf byteBuf) {
        messageId = GossipMessageId.createEmpty();
        byteBuf.readBytes(messageId.messageId());
        ttl = byteBuf.readUnsignedShort();
        dataType = new DataType(byteBuf.readUnsignedShort());
        byteBuf.readBytes(new byte[4]); // reading reserved header bytes

        data = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(data);
    }

    @Override
    public void accept(GossipEstablishedSession handler) {
        handler.handle(this);
    }
}