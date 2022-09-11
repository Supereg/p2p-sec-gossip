package de.tum.gossip.mocks;

import de.tum.gossip.net.packets.OutboundPacket;
import de.tum.gossip.p2p.GossipModule;
import de.tum.gossip.p2p.GossipPeerInfo;
import de.tum.gossip.p2p.packets.GossipPacketSpreadKnowledge;
import de.tum.gossip.p2p.protocol.EstablishedSession;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Andi on 08.09.22.
 */
public class MockModuleConnection {
    private class Session implements EstablishedSession {
        private static final AtomicInteger COUNTER = new AtomicInteger(0);

        private final int id = COUNTER.getAndIncrement();
        private final GossipPeerInfo peerInfo = MockPeerInfo.generate();

        private final GossipModule target;

        public Session(GossipModule target) {
            this.target = target;
        }

        @Override
        public String name() {
            return "MOCK-BRIDGE-SESSION-" + id;
        }

        @Override
        public GossipPeerInfo peerInfo() {
            return peerInfo;
        }

        @Override
        public <P extends OutboundPacket> void sendPacket(P packet, GenericFutureListener<? extends Future<? super Void>>[] genericFutureListeners) {
            if (!(packet instanceof GossipPacketSpreadKnowledge knowledgePacket)) {
                throw new RuntimeException("Encountered unexpected packet: " + packet);
            }

            var session = this.equals(sessionAToB) ? sessionBtoA : sessionAToB;
            target.handleIncomingKnowledgeSpread(session, knowledgePacket);
        }
    }

    private final GossipModule moduleA;
    private final GossipModule moduleB;

    private final Session sessionAToB;
    private final Session sessionBtoA;

    public MockModuleConnection(GossipModule moduleA, GossipModule moduleB) {
        this.moduleA = moduleA;
        this.moduleB = moduleB;
        this.sessionAToB = new Session(moduleB);
        this.sessionBtoA = new Session(moduleA);

        moduleA.registerEstablishedSession(sessionAToB);
        moduleB.registerEstablishedSession(sessionBtoA);
    }

    public void teardown() {
        moduleA.handleSessionDisconnect(sessionAToB);
        moduleB.handleSessionDisconnect(sessionBtoA);
    }
}