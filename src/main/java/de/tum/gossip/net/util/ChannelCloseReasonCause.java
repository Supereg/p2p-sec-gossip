package de.tum.gossip.net.util;

/**
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