package de.tum.gossip;

import de.tum.gossip.api.*;
import de.tum.gossip.api.packets.APIPacketGossipAnnounce;
import de.tum.gossip.api.packets.APIPacketGossipNotification;
import de.tum.gossip.api.packets.APIPacketGossipNotify;
import de.tum.gossip.api.packets.APIPacketGossipValidation;
import de.tum.gossip.net.ConnectionInitializer;
import de.tum.gossip.net.packets.EmptyPacketHandler;
import de.tum.gossip.p2p.util.DataType;
import de.tum.gossip.p2p.util.MessageNotificationId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by Andi on 21.06.22.
 */
class APITests {
    <T> T testSimpleInboundPacket(String hexString) throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();

        // we can use the EmptyPacketHandler as the inbound handler which calls the packet handler is not added and therefore the handler is never called!
        var initializer = new ConnectionInitializer(GossipAPILayer.PROTOCOL, EmptyPacketHandler::new, null, false);
        initializer.initChannel(channel);

        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(
                HexFormat
                        .ofDelimiter(":")
                        .parseHex(hexString)
        );

        assertTrue(channel.writeInbound(buf));

        return channel.readInbound();
    }

    <P> ByteBuf testSimpleOutboundPacket(P packet) throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();

        // we can use the EmptyPacketHandler as the inbound handler which calls the packet handler is not added and therefore the handler is never called!
        var initializer = new ConnectionInitializer(GossipAPILayer.PROTOCOL, EmptyPacketHandler::new, null, false);
        initializer.initChannel(channel);

        assertTrue(channel.writeOutbound(packet));

        ByteBuf buf = channel.readOutbound();

        ByteBuf other;
        while ((other = channel.readOutbound()) != null) {
            buf.writeBytes(other);
        }
        return buf;
    }

    @Test
    void announceTest() throws Exception {
        APIPacketGossipAnnounce packet = testSimpleInboundPacket(
                "00:0c:" // size: 12
                        + "01:f4:" // type: 500 (announcement)
                        + "30:" // ttl
                        + "00:" // reserved
                        + "00:01:" // data type
                        + "31:32:33:34" // data
        );

        assertEquals(0x30, packet.ttl);
        assertEquals(1, packet.dataType);
    }

    @Test
    void notifyTest() throws Exception {
        APIPacketGossipNotify packet = testSimpleInboundPacket(
                "00:08:" // size: 8
                        + "01:f5:" // type: 501 (notify)
                        + "00:00:" // reserved
                        + "00:01" // data type
        );

        assertEquals(1, packet.dataType);
    }

    @Test
    void validationTest() throws Exception {
        APIPacketGossipValidation packet = testSimpleInboundPacket(
                "00:08:" // size: 8
                        + "01:f7:" // type: 504 (validation)
                        + "05:39:" // messageId
                        + "00:01" // reserved + v
        );

        assertArrayEquals(new byte[] {0x05, 0x39}, packet.messageId.messageId());
        assertTrue(packet.valid);
    }

    @Test
    void notificationTest() throws Exception {
        ByteBuf buf = testSimpleOutboundPacket(new APIPacketGossipNotification(
                new MessageNotificationId(1),
                new DataType(2),
                new byte[2]
        ));

        var size = buf.readShort();
        var messageType = buf.readShort();
        var messageId = buf.readShort();
        var dataType = buf.readShort();
        var data = new byte[buf.readableBytes()];
        buf.readBytes(data);

        assertEquals(10, size);
        assertEquals(502, messageType);
        assertEquals(1, messageId);
        assertEquals(2, dataType);

        assertEquals(0, data[0]);
        assertEquals(0, data[1]);

        assertEquals(0, buf.readableBytes());

        buf.release();
    }
}