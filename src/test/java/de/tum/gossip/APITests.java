package de.tum.gossip;

import de.tum.gossip.api.*;
import de.tum.gossip.net.ConnectionInitializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by Andi on 21.06.22.
 */
class APITests {
    <T> T testSimpleInboundPacket(String hexString) throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        var initializer = new ConnectionInitializer(GossipAPILayer.PROTOCOL, GossipAPIConnectionHandler::new, false);
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
        var initializer = new ConnectionInitializer(GossipAPILayer.PROTOCOL, GossipAPIConnectionHandler::new, false);
        initializer.initChannel(channel);

        assertTrue(channel.writeOutbound(packet));

        ByteBuf buf = channel.readOutbound();

        ByteBuf other = null;
        while ((other = channel.readOutbound()) != null) {
            buf.writeBytes(other);
        }
        return buf;
    }

    @Test
    void announceTest() throws Exception {
        GossipAnnouncePacket packet = testSimpleInboundPacket(
                "00:0c:" // size: 12
                        + "01:f4:" // type: 500 (announcement)
                        + "30:" // ttl
                        + "00:" // reserved
                        + "00:01:" // data type
                        + "31:32:33:34" // data
        );

        assertEquals(0x30, packet.getTTL());
        assertEquals(1, packet.getDataType());
    }

    @Test
    void notifyTest() throws Exception {
        GossipNotifyPacket packet = testSimpleInboundPacket(
                "00:08:" // size: 8
                        + "01:f5:" // type: 501 (notify)
                        + "00:00:" // reserved
                        + "00:01" // data type
        );

        assertEquals(1, packet.getDataType());
    }

    @Test
    void validationTest() throws Exception {
        GossipValidationPacket packet = testSimpleInboundPacket(
                "00:08:" // size: 8
                        + "01:f7:" // type: 504 (validation)
                        + "05:39:" // messageId
                        + "00:01" // reserved + v
        );

        assertEquals(1337, packet.getMessageId());
        assertTrue(packet.isValid());
    }

    @Test
    void notificationTest() throws Exception {
        ByteBuf buf = testSimpleOutboundPacket(new GossipNotificationPacket(
                1,
                2,
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