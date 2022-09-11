package de.tum.gossip.net;

import de.tum.gossip.api.packets.APIPacketGossipAnnounce;
import de.tum.gossip.api.packets.APIPacketGossipNotification;
import de.tum.gossip.api.packets.APIPacketGossipNotify;
import de.tum.gossip.api.packets.APIPacketGossipValidation;
import de.tum.gossip.p2p.util.DataType;
import de.tum.gossip.p2p.util.MessageNotificationId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

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

        var packet = new APIPacketGossipAnnounce();
        packet.deserialize(msg);

        assertEquals(0x30, packet.ttl);
        assertEquals(1, packet.dataType);
    }

    @Test
    void decodeNotifyPacketTest() {
        ByteBuf msg = Unpooled.buffer();

        msg.writeBytes(
                HexFormat.ofDelimiter(":")
                        .parseHex("00:00:" // reserved
                                + "00:01" // data type
                        ));

        var packet = new APIPacketGossipNotify();
        packet.deserialize(msg);

        assertEquals(1, packet.dataType);
    }

    @Test
    void decodeValidationPacketTest() {
        ByteBuf msg = Unpooled.buffer();

        msg.writeBytes(
                HexFormat.ofDelimiter(":")
                        .parseHex("05:39:" // messageId
                                + "00:01" // reserved + v
                        ));

        var packet = new APIPacketGossipValidation();
        packet.deserialize(msg);

        assertArrayEquals(new byte[] {0x05, 0x39}, packet.messageId.messageId());
        assertTrue(packet.valid);
    }

    @Test
    void encodeNotificationPacketTest() {
        var packet = new APIPacketGossipNotification(new MessageNotificationId(0x10), new DataType(0x20), HexFormat.ofDelimiter(":").parseHex("31:32:33:34"));

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