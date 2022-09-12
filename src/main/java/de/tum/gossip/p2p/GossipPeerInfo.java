package de.tum.gossip.p2p;

import de.tum.gossip.crypto.PeerIdentity;

import java.security.interfaces.RSAPublicKey;
import java.util.Objects;

/**
 * Created by Andi on 06.07.22.
 */
public record GossipPeerInfo(
        PeerIdentity identity,
        RSAPublicKey publicKey
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GossipPeerInfo that = (GossipPeerInfo) o;
        return Objects.equals(identity, that.identity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identity);
    }

    @Override
    public String toString() {
        return identity.toString();
    }
}
