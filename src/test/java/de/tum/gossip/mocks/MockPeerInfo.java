package de.tum.gossip.mocks;

import de.tum.gossip.crypto.HostKey;
import de.tum.gossip.p2p.GossipPeerInfo;

/**
 * Created by Andi on 11.09.22.
 */
public class MockPeerInfo {
    public static GossipPeerInfo generate() {
        return from(HostKey.generate());
    }

    public static GossipPeerInfo from(HostKey hostKey) {
        return new GossipPeerInfo(hostKey.identity, hostKey.publicKey);
    }
}