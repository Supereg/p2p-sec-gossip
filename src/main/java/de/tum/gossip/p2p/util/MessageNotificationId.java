package de.tum.gossip.p2p.util;

import de.tum.gossip.crypto.GossipCrypto;

import java.nio.ByteBuffer;
import java.util.Arrays;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageNotificationId that = (MessageNotificationId) o;
        return Arrays.equals(messageId, that.messageId);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(messageId);
    }

    @Override
    public String toString() {
        return "NotificationId{" +
                GossipCrypto.formatHex(messageId) +
                '}';
    }
}
