package de.tum.gossip.mocks;

import de.tum.gossip.api.APIConnection;
import de.tum.gossip.api.packets.APIPacketGossipNotification;
import de.tum.gossip.net.packets.OutboundPacket;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Andi on 06.09.22.
 */
public class MockAPIConnection implements APIConnection {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    public final BlockingQueue<APIPacketGossipNotification> notificationQueue = new LinkedBlockingQueue<>();
    private final int id = COUNTER.getAndIncrement();

    @Override
    public String name() {
        return "MOCK-API-" + id;
    }

    @Override
    public <Packet extends OutboundPacket> void sendPacket(Packet packet, GenericFutureListener<? extends Future<? super Void>>[] genericFutureListeners) {
        if (!(packet instanceof APIPacketGossipNotification apiPacket)) {
            throw new RuntimeException("Encountered unexpected packet: " + packet);
        }

        notificationQueue.add(apiPacket);
    }
}