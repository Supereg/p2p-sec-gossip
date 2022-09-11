package de.tum.gossip.crypto;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * A `HostKey` instances represents the host key of a peer.
 * The hostkey is a 4096-bit long RSA public-private key-pair.
 */
public class HostKey {
    public final RSAPublicKey publicKey;
    public final RSAPrivateKey privateKey;

    /**
     * The identity of the peer's host key.
     * We consider the identity as the SHA256 has of the public part of the host key.
     */
    public final PeerIdentity identity;

    public HostKey(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;

        this.identity = new PeerIdentity(publicKey);
    }
}