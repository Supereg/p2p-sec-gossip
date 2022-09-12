package de.tum.gossip.p2p.protocol;

import de.tum.gossip.net.packets.PacketSendable;
import de.tum.gossip.p2p.GossipPeerInfo;

/**
 * A generic instance of an established session with an remote gossip peer.
 * <p>
 * Created by Andi on 07.09.22.
 */
public interface EstablishedSession extends PacketSendable {
    String name();

    GossipPeerInfo peerInfo();

    String ipAddress();

    /**
     * If this is {@code ture}, this session was established at the server side by an incoming connection from a remote peer.
     */
    boolean isServerBound();
}
