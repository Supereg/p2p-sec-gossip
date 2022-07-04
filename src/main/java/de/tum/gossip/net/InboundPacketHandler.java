package de.tum.gossip.net;

/**
 * Created by Andi on 21.06.22.
 */
public interface InboundPacketHandler {
    // TODO rename methods!
    void onConnect(ChannelInboundHandler channel); // TODO pass channel reference as argument!

    void onDisconnect(); // TODO reason?
}