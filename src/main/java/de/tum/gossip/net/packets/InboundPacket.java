package de.tum.gossip.net.packets;

import io.netty.buffer.ByteBuf;

/**
 * Created by Andi on 21.06.22.
 */
public interface InboundPacket<Handler extends InboundPacketHandler> extends SomePacket {
    void deserialize(ByteBuf byteBuf);

    void accept(Handler handler);

    default boolean applicableHandler(InboundPacketHandler type) {
        try {
            //noinspection unchecked,unused
            Handler h = (Handler) type;
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }
}