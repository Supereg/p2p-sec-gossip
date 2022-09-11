package de.tum.gossip.p2p.protocol;

import de.tum.gossip.net.ChannelInboundHandler;
import de.tum.gossip.net.packets.InboundPacketHandler;
import de.tum.gossip.p2p.packets.GossipPacketDisconnect;

/**
 * Created by Andi on 07.07.22.
 */
public interface GossipPacketHandler extends InboundPacketHandler {
    ChannelInboundHandler channel();

    void handle(GossipPacketDisconnect packet);
}