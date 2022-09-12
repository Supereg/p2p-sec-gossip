package de.tum.gossip.net.packets;

import io.netty.buffer.ByteBuf;

/**
 * Some packet, that can be used as an outbound packet.
 * <p>
 * Created by Andi on 21.06.22.
 */
public interface OutboundPacket extends SomePacket {
    void serialize(ByteBuf byteBuf);
}