package de.tum.gossip.p2p.packets;

import de.tum.gossip.net.packets.Packet;
import de.tum.gossip.p2p.protocol.GossipClientHandshakeListener;
import io.netty.buffer.ByteBuf;

/**
 * Created by Andi on 07.07.22.
 */
public class GossipHandshakeComplete implements Packet<GossipClientHandshakeListener> {
    public GossipHandshakeComplete() {}

    @Override
    public void serialize(ByteBuf byteBuf) {}

    @Override
    public void deserialize(ByteBuf byteBuf) {}

    @Override
    public void accept(GossipClientHandshakeListener handler) {
        handler.handle(this);
    }

}