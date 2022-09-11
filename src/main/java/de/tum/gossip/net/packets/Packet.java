package de.tum.gossip.net.packets;

/**
 * Created by Andi on 06.07.22.
 */
public interface Packet<Handler extends InboundPacketHandler> extends InboundPacket<Handler>, OutboundPacket {}
