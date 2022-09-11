package de.tum.gossip.crypto;

import com.google.common.base.Preconditions;

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

import static de.tum.gossip.crypto.GossipCrypto.SHA256_HASH_BYTES_LENGTH;

/**
 * Instances of this class capture the identity of a peer.
 */
public class PeerIdentity {
    private final byte[] bytes;

    public PeerIdentity(RSAPublicKey key) {
        bytes = GossipCrypto.publicKeyIdentity(key);
    }

    public PeerIdentity(byte[] bytes) {
        Preconditions.checkState(bytes.length == SHA256_HASH_BYTES_LENGTH, "Illegal identity length!");
        this.bytes = bytes;
    }

    public String hexString() {
        return GossipCrypto.formatHex(bytes);
    }

    public byte[] rawBytes() {
        return bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerIdentity that = (PeerIdentity) o;
        return Arrays.equals(bytes, that.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return "PeerIdentity{" +
                "identity=" + hexString() +
                '}';
    }
}