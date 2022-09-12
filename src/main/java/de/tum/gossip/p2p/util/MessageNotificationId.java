package de.tum.gossip.p2p.util;

import de.tum.gossip.crypto.GossipCrypto;

import java.nio.ByteBuffer;

/**
 * The identifier to identity pairs of {@link de.tum.gossip.api.packets.APIPacketGossipNotification}
 * and {@link de.tum.gossip.api.packets.APIPacketGossipValidation}.
 * The identifier is only used within the API-protocol layer.
 * Also see {@link GossipMessage}.
 *
 * @param messageId - 2 Bytes message Id.
 */
public record MessageNotificationId(byte[] messageId) {
    public MessageNotificationId(int messageId) {
        this(ByteBuffer.allocate(2).putShort((short) messageId).array());
    }

    public static MessageNotificationId createEmpty() {
        return new MessageNotificationId(new byte[2]);
    }

    @Override
    public String toString() {
        return "NotificationId{" +
                GossipCrypto.formatHex(messageId) +
                '}';
    }
}
