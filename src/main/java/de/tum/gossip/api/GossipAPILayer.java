package de.tum.gossip.api;

import de.tum.gossip.ConfigurationFile;
import de.tum.gossip.net.ProtocolDescription;
import de.tum.gossip.net.TCPServer;
import io.netty.channel.EventLoopGroup;

/**
 * Created by Andi on 21.06.22.
 */
public class GossipAPILayer {
    public static final ProtocolDescription PROTOCOL = new ProtocolDescription()
            .registerInbound(500, GossipAnnouncePacket::new)
            .registerInbound(501, GossipNotifyPacket::new)
            .registerOutbound(502, GossipNotificationPacket::new)
            .registerInbound(503, GossipValidationPacket::new);

    private final TCPServer server;

    public GossipAPILayer(ConfigurationFile configuration, EventLoopGroup eventLoopGroup) {
        server = PROTOCOL.makeServer(configuration.api_address, 0, eventLoopGroup, GossipAPIConnectionHandler::new);
    }

    public void run() {
        server.run();
    }
}