package de.tum.gossip.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
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

    static {
        GossipCrypto.init();
    }

    public HostKey(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;

        this.identity = new PeerIdentity(publicKey);
    }

    public static HostKey generate() {
        KeyPair keyPair;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
            generator.initialize(4096, GossipCrypto.SECURE_RANDOM);
            keyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            // we know that every Java implementation supports EC and provider
            throw new RuntimeException(e);
        }

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        return new HostKey(publicKey, privateKey);
    }
}