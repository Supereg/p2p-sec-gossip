package de.tum.gossip.api;

import com.google.common.base.Preconditions;
import de.tum.gossip.api.packets.APIPacketGossipAnnounce;
import de.tum.gossip.api.packets.APIPacketGossipNotify;
import de.tum.gossip.api.packets.APIPacketGossipValidation;
import de.tum.gossip.api.packets.GossipAPIPacket;
import de.tum.gossip.net.util.ChannelCloseReason;
import de.tum.gossip.net.ChannelInboundHandler;
import de.tum.gossip.net.packets.OutboundPacket;
import de.tum.gossip.p2p.GossipException;
import de.tum.gossip.p2p.GossipModule;
import de.tum.gossip.p2p.util.DataType;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.UUID;

/**
 * Created by Andi on 27.06.22.
 */
public class GossipAPIConnection implements GossipAPIPacketHandler, APIConnection {
    private final Logger logger = LogManager.getLogger(GossipAPIConnection.class);
    private final UUID uuid;
    private final GossipModule gossipModule;

    private ChannelInboundHandler channel;

    public GossipAPIConnection(GossipModule gossipModule) {
        this.uuid = UUID.randomUUID();
        this.gossipModule = gossipModule;
    }

    @Override
    public String name() {
        return channel.getHandle().remoteAddress().toString();
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public void onConnect(ChannelInboundHandler channel) {
        this.channel = channel;

        // for API-layer, handshake is considered successful when finishing the TCP handshake
        channel.handshakePromise().setSuccess(channel);
    }

    @Override
    public void onDisconnect(ChannelCloseReason reason) {
        this.channel = null;
        gossipModule.handleDisconnectedAPIClient(this);
    }

    @Override
    public void handle(APIPacketGossipAnnounce packet) {
        var type = new DataType(packet.dataType);
        try {
            gossipModule.spreadInformation(this, packet.ttl, type, packet.data);
        } catch (GossipException e) {
            channel.close(new ChannelCloseReason.Message(e.getMessage()));
        }
    }

    @Override
    public void handle(APIPacketGossipNotify packet) {
        var type = new DataType(packet.dataType);
        gossipModule.registerNotification(this, type);
    }

    @Override
    public void handle(APIPacketGossipValidation packet) {
        try {
            gossipModule.receiveMessageValidation(packet.messageId, packet.valid);
        } catch (GossipException ignored) {
            // fine, ttl ran out or packet was evicted due to other issues (space, invalidity)
        }
    }

    @Override
    public <Packet extends OutboundPacket> void sendPacket(
            Packet packet,
            GenericFutureListener<? extends Future<? super Void>>[] genericFutureListeners
    ) {
        Preconditions.checkNotNull(channel);
        Preconditions.checkState(packet instanceof GossipAPIPacket);
        channel.sendPacket(packet, genericFutureListeners);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GossipAPIConnection that = (GossipAPIConnection) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}