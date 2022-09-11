package de.tum.gossip.mocks;

import de.tum.gossip.net.packets.EmptyPacketHandler;
import de.tum.gossip.net.packets.Packet;
import io.netty.buffer.ByteBuf;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by Andi on 07.07.22.
 */
public class ExamplePacket implements Packet<EmptyPacketHandler> {
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