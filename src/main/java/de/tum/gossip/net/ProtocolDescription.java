package de.tum.gossip.net;

import com.google.common.base.Preconditions;
import io.netty.channel.EventLoopGroup;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Created by Andi on 21.06.22.
 */
public class ProtocolDescription {
    private final Map<Integer, Supplier<? extends InboundPacket<? extends InboundPacketHandler>>> inboundPacketSuppliers = new HashMap<>();
    private final Map<Class<? extends OutboundPacket>, Integer> outboundPacketIds = new HashMap<>();

    public ProtocolDescription() {}

    public <P extends InboundPacket<? extends InboundPacketHandler>> ProtocolDescription registerInbound(Integer packetId, Supplier<P> packetConstructor) {
        var previous = inboundPacketSuppliers.put(packetId, packetConstructor);
        Preconditions.checkState(previous == null, "Tried overwriting existing inbound packet for packetId %d!", packetId);
        return this;
    }

    public <P extends OutboundPacket> ProtocolDescription registerOutbound(Integer packetId, Supplier<P> packetConstructor) {
        // Java doesn't provide a way to access/use the generic type `P`. Therefore, we
        // do this workaround below, where we instantiate a single packet to derive its class type!
        // This is okay, as the provided Supplier shouldn't create any side effects.
        var instance = packetConstructor.get();
        var previous = outboundPacketIds.put(instance.getClass(), packetId);
        Preconditions.checkState(previous == null, "Tried overwriting existing outbound packet for packetId %d!", packetId);
        return this;
    }

    public <P extends InboundPacket<? extends InboundPacketHandler> & OutboundPacket> ProtocolDescription registerInboundAndOutbound(
            Integer packetId,
            Supplier<P> packetConstructor
    ) {
        registerInbound(packetId, packetConstructor);
        registerOutbound(packetId, packetConstructor);
        return this;
    }

    public Optional<InboundPacket<? extends InboundPacketHandler>> newPacketInstanceFromInbound(Integer packetId) {
        Supplier<? extends InboundPacket<? extends InboundPacketHandler>> packetSupplier;

        packetSupplier = inboundPacketSuppliers.get(packetId);

        if (packetSupplier == null) {
            return Optional.empty();
        }

        InboundPacket<? extends InboundPacketHandler> packet = packetSupplier.get();

        return Optional.of(packet);
    }

    public <P extends OutboundPacket> Optional<Integer> packetIdFromPacket(P packet) {
        Integer packetId = outboundPacketIds.get(packet.getClass());
        return Optional.ofNullable(packetId);
    }

    public <Handler extends InboundPacketHandler> TCPServer makeServer(int port, EventLoopGroup eventLoopGroup, Supplier<Handler> defaultHandler) {
        return this.makeServer(null, port, eventLoopGroup, defaultHandler);
    }

    public <Handler extends InboundPacketHandler> TCPServer makeServer(@Nullable String hostname, int port, EventLoopGroup eventLoopGroup, Supplier<Handler> defaultHandler) {
        return new TCPServer(hostname, port, eventLoopGroup, this, defaultHandler);
    }

    public <Handler extends InboundPacketHandler> TCPClient makeClient(String hostname, int port, EventLoopGroup eventLoopGroup, Supplier<Handler> defaultHandler) {
        return new TCPClient(hostname, port, eventLoopGroup, this, defaultHandler);
    }
}