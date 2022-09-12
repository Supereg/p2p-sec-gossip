package de.tum.gossip.p2p.util;

import de.tum.gossip.crypto.GossipCrypto;

import java.util.Arrays;

/**
 * The identifier to identity {@link de.tum.gossip.p2p.packets.GossipPacketSpreadKnowledge} instances.
 * The identifier is only used within the p2p-protocol layer.
 * Also see {@link GossipMessage}.
 * <p>
 * @param messageId - 8 bytes of message id.
 * Created by Andi on 07.07.22.
 */
public record GossipMessageId(byte[] messageId) {
    public static GossipMessageId createEmpty() {
        return new GossipMessageId(new byte[8]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GossipMessageId that = (GossipMessageId) o;
        return Arrays.equals(messageId, that.messageId);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(messageId);
    }

    @Override
    public String toString() {
        return "MessageId{" +
                GossipCrypto.formatHex(messageId) +
                '}';
    }
}
