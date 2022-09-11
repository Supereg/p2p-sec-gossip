package de.tum.gossip.p2p.util;

import de.tum.gossip.net.util.Hex;

/**
 * Created by Andi on 07.07.22.
 */
public record GossipMessageId(byte[] messageId) {
    public static GossipMessageId createEmpty() {
        return new GossipMessageId(new byte[8]);
    }

    @Override
    public String toString() {
        return "MessageId{" +
                Hex.byteArrayToHex(messageId) +
                '}';
    }
}
