package de.tum.gossip.net;

import com.google.common.base.Preconditions;
import de.tum.gossip.net.packets.InboundPacket;
import de.tum.gossip.net.packets.InboundPacketHandler;
import de.tum.gossip.net.packets.OutboundPacket;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLException;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The description of packets supported by a specific protocol.
 * <p>
 * Created by Andi on 21.06.22.
 */
public class ProtocolDescription implements Cloneable {
    private HashMap<Integer, Supplier<? extends InboundPacket<? extends InboundPacketHandler>>> inboundPacketSuppliers = new HashMap<>();
    private HashMap<Class<? extends OutboundPacket>, Integer> outboundPacketIds = new HashMap<>();

    @Nullable
    private SslContext sslContext;

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

    public ProtocolDescription withSslContext(SslContextBuilder builder) {
        try {
            return withSslContext(builder.build());
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    public ProtocolDescription withSslContext(@NonNull SslContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    public @Nullable SslContext getSslContext() {
        return sslContext;
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


    @Override
    public ProtocolDescription clone() {
        try {
            ProtocolDescription clone = (ProtocolDescription) super.clone();

            //noinspection unchecked
            clone.inboundPacketSuppliers = (HashMap<Integer, Supplier<? extends InboundPacket<? extends InboundPacketHandler>>>) inboundPacketSuppliers.clone();
            //noinspection unchecked
            clone.outboundPacketIds = (HashMap<Class<? extends OutboundPacket>, Integer>) outboundPacketIds.clone();

            clone.sslContext = null;

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}