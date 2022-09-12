package de.tum.gossip.net.util;

/**
 * Describes the state of either a {@link de.tum.gossip.net.TCPServer} or a {@link de.tum.gossip.net.TCPClient}.
 * <p>
 * Created by Andi on 11.09.22.
 */
public enum ChannelState {
    FREE,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
}