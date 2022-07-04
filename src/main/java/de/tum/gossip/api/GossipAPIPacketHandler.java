package de.tum.gossip.api;

import de.tum.gossip.net.InboundPacketHandler;

/**
 * Created by Andi on 21.06.22.
 */
public interface GossipAPIPacketHandler extends InboundPacketHandler {
    void handle(GossipAnnouncePacket packet);

    void handle(GossipNotifyPacket packet);

    void handle(GossipValidationPacket packet);
}