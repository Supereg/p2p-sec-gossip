package de.tum.gossip.net.packets;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Created by Andi on 06.09.22.
 */
public interface PacketSendable {
    default <P extends OutboundPacket> void sendPacket(
            P packet
    ) {
        // noinspection unchecked
        sendPacket(packet, new GenericFutureListener[] {});
    }

    <P extends OutboundPacket> void sendPacket(
            P packet,
            GenericFutureListener<? extends Future<? super Void>>[] genericFutureListeners
    );
}
