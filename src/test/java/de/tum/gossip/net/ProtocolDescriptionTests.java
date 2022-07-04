package de.tum.gossip.net;

import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by Andi on 04.07.22.
 */
public class ProtocolDescriptionTests {
    // TODO move to generic mock package for test target!
    static class ExamplePacket implements OutboundPacket, InboundPacket<EmptyPacketHandler> {
        @Override
        public void serialize(ByteBuf byteBuf) {
            byteBuf.writeShort(42);
        }

        @Override
        public void deserialize(ByteBuf byteBuf) {
            assertEquals(42, byteBuf.readShort());
        }

        @Override
        public void accept(EmptyPacketHandler handler) {
            handler.handle(this);
        }
    }

    @Test
    void testInboundPacketRegistration() {
        var description = new ProtocolDescription()
                .registerInbound(2, ExamplePacket::new);

        assertThrows(IllegalStateException.class, () -> description.registerInbound(2, ExamplePacket::new));

        assertEquals(Optional.empty(), description.packetIdFromPacket(new ExamplePacket()));

        var packet = description.newPacketInstanceFromInbound(2);
        assertTrue(packet.isPresent());

        assertEquals(ExamplePacket.class, packet.get().getClass());
    }

    @Test
    void testOutboundPacketRegistration() {
        var description = new ProtocolDescription()
                .registerOutbound(2, ExamplePacket::new);

        assertThrows(IllegalStateException.class, () -> description.registerOutbound(2, ExamplePacket::new));

        assertEquals(Optional.of(2), description.packetIdFromPacket(new ExamplePacket()));
    }
}