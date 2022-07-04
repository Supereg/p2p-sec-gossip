package de.tum.gossip.api;

import de.tum.gossip.net.ChannelInboundHandler;

/**
 * Created by Andi on 27.06.22.
 */
public class GossipAPIConnectionHandler implements GossipAPIPacketHandler {
    @Override
    public void onConnect(ChannelInboundHandler channel) {}

    @Override
    public void onDisconnect() {}

    @Override
    public void handle(GossipAnnouncePacket packet) {}

    @Override
    public void handle(GossipNotifyPacket packet) {}

    @Override
    public void handle(GossipValidationPacket packet) {}
}