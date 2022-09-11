package de.tum.gossip.mocks;

import com.google.common.base.Preconditions;
import de.tum.gossip.crypto.GossipCrypto;
import de.tum.gossip.crypto.HostKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Andi on 11.09.22.
 */
public class MockHostKey {
    private static AtomicInteger fileIndex = new AtomicInteger(0);

    static {
        GossipCrypto.init();
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

    public static File writeIntoDirectory(HostKey hostKey, File parent) {
        Preconditions.checkState(parent.isDirectory());

        var file = new File(parent, "hostkey-" + fileIndex.incrementAndGet() + ".pem");
        GossipCrypto.writeHostKey(hostKey, file);
        return file;
    }
}