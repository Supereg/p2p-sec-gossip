package de.tum.gossip.p2p.util;

import de.tum.gossip.net.util.Hex;

import java.nio.ByteBuffer;

/**
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
                Hex.byteArrayToHex(messageId) +
                '}';
    }
}
