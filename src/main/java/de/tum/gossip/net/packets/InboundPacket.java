package de.tum.gossip.net.packets;

import io.netty.buffer.ByteBuf;

/**
 * Some inbound packet. An inbound packet must always define the type of packet handler that
 * is capable of handling the implementing packet type. We employ a visitor pattern to
 * call the respective packet handler.
 * <p>
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