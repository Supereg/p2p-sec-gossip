package de.tum.gossip.api;

import de.tum.gossip.api.packets.APIPacketGossipAnnounce;
import de.tum.gossip.api.packets.APIPacketGossipNotify;
import de.tum.gossip.api.packets.APIPacketGossipValidation;
import de.tum.gossip.net.packets.InboundPacketHandler;

/**
 * Created by Andi on 21.06.22.
 */
public interface GossipAPIPacketHandler extends InboundPacketHandler {
    void handle(APIPacketGossipAnnounce packet);

    void handle(APIPacketGossipNotify packet);

    void handle(APIPacketGossipValidation packet);
}