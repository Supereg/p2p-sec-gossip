package de.tum.gossip.api;

import de.tum.gossip.ConfigurationFile;
import de.tum.gossip.api.packets.APIPacketGossipAnnounce;
import de.tum.gossip.api.packets.APIPacketGossipNotification;
import de.tum.gossip.api.packets.APIPacketGossipNotify;
import de.tum.gossip.api.packets.APIPacketGossipValidation;
import de.tum.gossip.net.ProtocolDescription;
import de.tum.gossip.net.TCPServer;
import de.tum.gossip.p2p.GossipModule;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by Andi on 21.06.22.
 */
public class GossipAPILayer {
    public static final ProtocolDescription PROTOCOL = new ProtocolDescription()
            .registerInbound(500, APIPacketGossipAnnounce::new)
            .registerInbound(501, APIPacketGossipNotify::new)
            .registerOutbound(502, APIPacketGossipNotification::new)
            .registerInbound(503, APIPacketGossipValidation::new);

    private final Logger logger = LogManager.getLogger(GossipAPILayer.class);
    private final TCPServer server;

    public GossipAPILayer(ConfigurationFile configuration, EventLoopGroup eventLoopGroup, GossipModule gossipModule) {
        server = PROTOCOL.makeServer(configuration.api_address(), configuration.api_port(), eventLoopGroup, () -> new GossipAPIConnection(gossipModule));
    }

    public ChannelFuture run() {
        return server.bind().addListener(future -> {
            if (future.isSuccess()) {
                logger.info("API listening on {}:{}", server.hostname, server.port);
            } else {
                logger.error("Failed to bind API server!");
            }
        });
    }

    public ChannelFuture shutdown() {
        return server.stop();
    }
}