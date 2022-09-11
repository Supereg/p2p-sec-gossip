package de.tum.gossip.net.packets;

import de.tum.gossip.net.util.ChannelCloseReason;
import de.tum.gossip.net.ChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by Andi on 04.07.22.
 */
public class EmptyPacketHandler implements InboundPacketHandler {
    private final Logger logger = LogManager.getLogger(EmptyPacketHandler.class);

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public void onConnect(ChannelInboundHandler channel) {}

    @Override
    public void onDisconnect(ChannelCloseReason reason) {}

    public void handle(Object any) {}
}