package de.tum.gossip.mocks;

import de.tum.gossip.net.packets.OutboundPacket;
import de.tum.gossip.p2p.GossipPeerInfo;
import de.tum.gossip.p2p.packets.GossipPacketSpreadKnowledge;
import de.tum.gossip.p2p.protocol.EstablishedSession;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Andi on 08.09.22.
 */
public class MockEstablishedSession implements EstablishedSession { // TODO unused?
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final int id = COUNTER.getAndIncrement();
    private final GossipPeerInfo peerInfo = MockPeerInfo.generate();

    public final BlockingQueue<GossipPacketSpreadKnowledge> notificationQueue = new LinkedBlockingQueue<>();

    @Override
    public String name() {
        return "MOCK-SESSION-" + id;
    }

    @Override
    public GossipPeerInfo peerInfo() {
        return peerInfo;
    }

    @Override
    public <Packet extends OutboundPacket> void sendPacket(Packet packet, GenericFutureListener<? extends Future<? super Void>>[] genericFutureListeners) {
        if (!(packet instanceof GossipPacketSpreadKnowledge knowledgePacket)) {
            throw new RuntimeException("Encountered unexpected packet: " + packet);
        }

        notificationQueue.add(knowledgePacket);
    }
}