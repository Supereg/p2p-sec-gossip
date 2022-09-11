package de.tum.gossip.api;

import de.tum.gossip.net.packets.PacketSendable;

/**
 * Created by Andi on 07.09.22.
 */
public interface APIConnection extends PacketSendable {
    String name();
}
