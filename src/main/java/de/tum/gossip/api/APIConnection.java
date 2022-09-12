package de.tum.gossip.api;

import de.tum.gossip.net.packets.PacketSendable;

/**
 * A generic instance of an API Connection.
 * <p>
 * Created by Andi on 07.09.22.
 */
public interface APIConnection extends PacketSendable {
    String name();
}
