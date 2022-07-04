package de.tum.gossip.net;

import io.netty.buffer.ByteBuf;

/**
 * Created by Andi on 21.06.22.
 */
public interface InboundPacket<Handler extends InboundPacketHandler> extends Packet {
    void deserialize(ByteBuf byteBuf);

    void accept(Handler handler);
}