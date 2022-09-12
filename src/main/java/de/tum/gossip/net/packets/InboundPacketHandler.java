package de.tum.gossip.net.packets;

import de.tum.gossip.net.util.ChannelCloseReason;
import de.tum.gossip.net.ChannelInboundHandler;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * The required interface for any inbound packet handler. Defined for a {@link InboundPacket}.
 * <p>
 * Created by Andi on 21.06.22.
 */
public interface InboundPacketHandler {
    /**
     * This method gives access to the Logger instance of the packet handler.
     */
    Logger logger();

    /**
     * This method is called once a new channel is connected.
     * This method will also be called when a new packet handler is set for a currently open {@link ChannelInboundHandler}
     * using {@link ChannelInboundHandler#replacePacketHandler(InboundPacketHandler)}
     * or {@link ChannelInboundHandler#replacePacketHandler(Supplier)}.
     *
     * @param channel - The opened channel for which this packet handler was instantiated for.
     */
    void onConnect(ChannelInboundHandler channel);

    /**
     * This method is called once the packet handler is removed/deactivated, or phrased differently,
     * if the handler is replaced by some new packet handler.
     * It can be used to perform certain cleanup operations before enabling the new packet handler.
     * The method is not called when the channel disconnects.
     */
    default void onHandlerRemove() {};

    /**
     * This method is called once the channel is closed.
     * It is provided with the respective ChannelCloseReason.
     * @param reason - The ChannelCloseReason passed to the channel `close(...)` method.
     */
    void onDisconnect(ChannelCloseReason reason);
}