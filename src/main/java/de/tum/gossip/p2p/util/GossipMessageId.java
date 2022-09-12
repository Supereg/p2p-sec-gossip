package de.tum.gossip.p2p.util;

import de.tum.gossip.crypto.GossipCrypto;

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
                GossipCrypto.formatHex(messageId) +
                '}';
    }
}
