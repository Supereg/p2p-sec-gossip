package de.tum.gossip.net;

import de.tum.gossip.api.GossipAnnouncePacket;
import de.tum.gossip.api.GossipNotificationPacket;
import de.tum.gossip.api.GossipNotifyPacket;
import de.tum.gossip.api.GossipValidationPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by Andi on 21.06.22.
 */
class APIPacketTests {
    @Test
    void decodeAnnouncePacketTest() {
        ByteBuf msg = Unpooled.buffer();

        msg.writeBytes(
                HexFormat.ofDelimiter(":")
                        .parseHex("30:" // ttl
                                + "00:" // reserved
                                + "00:01:" // data type
                                + "31:32:33:34" // data
                        ));

        var packet = new GossipAnnouncePacket();
        packet.deserialize(msg);

        assertEquals(0x30, packet.getTTL());
        assertEquals(1, packet.getDataType());
    }

    @Test
    void decodeNotifyPacketTest() {
        ByteBuf msg = Unpooled.buffer();

        msg.writeBytes(
                HexFormat.ofDelimiter(":")
                        .parseHex("00:00:" // reserved
                                + "00:01" // data type
                        ));

        var packet = new GossipNotifyPacket();
        packet.deserialize(msg);

        assertEquals(1, packet.getDataType());
    }

    @Test
    void decodeValidationPacketTest() {
        ByteBuf msg = Unpooled.buffer();

        msg.writeBytes(
                HexFormat.ofDelimiter(":")
                        .parseHex("05:39:" // messageId
                                + "00:01" // reserved + v
                        ));

        var packet = new GossipValidationPacket();
        packet.deserialize(msg);

        assertEquals(1337, packet.getMessageId());
        assertTrue(packet.isValid());
    }

    @Test
    void encodeNotificationPacketTest() {
        var packet = new GossipNotificationPacket();

        packet.setMessageId(0x10);
        packet.setDataType(0x20);
        packet.setData(HexFormat.ofDelimiter(":").parseHex("31:32:33:34"));

        ByteBuf byteBuf = Unpooled.buffer();
        packet.serialize(byteBuf);

        assertEquals(0x10, byteBuf.readShort());
        assertEquals(0x20, byteBuf.readShort());
        assertEquals(0x31, byteBuf.readByte());
        assertEquals(0x32, byteBuf.readByte());
        assertEquals(0x33, byteBuf.readByte());
        assertEquals(0x34, byteBuf.readByte());
    }
}