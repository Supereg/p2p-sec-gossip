package de.tum.gossip.net;

import io.netty.buffer.ByteBuf;

/**
 * Created by Andi on 21.06.22.
 */
public interface OutboundPacket extends Packet {
    void serialize(ByteBuf byteBuf);
}