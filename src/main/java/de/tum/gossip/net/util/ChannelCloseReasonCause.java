package de.tum.gossip.net.util;

import de.tum.gossip.net.ChannelInboundHandler;

/**
 * An exception that carries information about a {@link ChannelCloseReason}.
 * The exception is used within the {@link ChannelInboundHandler#handshakeFuture()} when failing
 * due to a closed channel.
 * <p>
 * Created by Andi on 10.09.22.
 */
public class ChannelCloseReasonCause extends Exception {
    public final ChannelCloseReason channelCloseReason;

    public ChannelCloseReasonCause(ChannelCloseReason channelCloseReason) {
        super("Remote disconnected with reason: " + channelCloseReason.getMessage());
        channelCloseReason.asExceptionCause().ifPresent(this::initCause);
        this.channelCloseReason = channelCloseReason;
    }
}