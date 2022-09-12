package de.tum.gossip.net.packets;

/**
 * A combination of {@link InboundPacket} and {@link OutboundPacket}.
 * <p>
 * Created by Andi on 06.07.22.
 */
public interface Packet<Handler extends InboundPacketHandler> extends InboundPacket<Handler>, OutboundPacket {}
