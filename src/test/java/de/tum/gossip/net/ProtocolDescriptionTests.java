package de.tum.gossip.net;

import de.tum.gossip.mocks.ExamplePacket;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by Andi on 04.07.22.
 */
public class ProtocolDescriptionTests {
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