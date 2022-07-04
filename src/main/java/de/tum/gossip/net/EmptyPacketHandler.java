package de.tum.gossip.net;

/**
 * Created by Andi on 04.07.22.
 */
public class EmptyPacketHandler implements InboundPacketHandler {
    @Override
    public void onConnect(ChannelInboundHandler channel) {}

    @Override
    public void onDisconnect() {}

    public void handle(Object any) {}
}