package de.tum.gossip.p2p;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import de.tum.gossip.crypto.PeerIdentity;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;

/**
 * Created by Andi on 06.07.22.
 */
public class PeerIdentityStorage {
    private final File storageFolder;
    private final LoadingCache<PeerIdentity, RSAPublicKey> identityCache;

    // TODO also store some attributes like last known connection information?

    public PeerIdentityStorage() {
        this(new File("./identities"));
    }

    /**
     * Creates a new PeerIdentityStorage.
     * @param storageFolder - The folder to load and store peer identities. Folder will be created if it doesn't exist.
     */
    public PeerIdentityStorage(File storageFolder) {
        this(storageFolder, false);
    }

    /**
     * Creates a new PeerIdentityStorage.
     * @param storageFolder - The folder to load and store peer identities. Folder will be created if it doesn't exist.
     * @param disableCaching - Mainly used for testing. Allows to disable local caches, resulting in a read from
     *                       disk on every retrieval request.
     */
    public PeerIdentityStorage(File storageFolder, boolean disableCaching) {
        this.storageFolder = storageFolder;
        Preconditions.checkState(!storageFolder.exists() || storageFolder.isDirectory());

        if (!storageFolder.exists()) {
            var result = storageFolder.mkdirs();
            Preconditions.checkState(result, "Failed to create PeerIdentityStorage folder at " + storageFolder.getAbsolutePath());
        }

        var builder = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofHours(1))
                .scheduler(Scheduler.systemScheduler());

        if (disableCaching) {
            builder
                    .maximumSize(0) // ensures elements are discarded immediately
                    .executor(Runnable::run); // ensures evicting task is execute synchronously
        }

        identityCache = builder.build(new IdentityLoader());
    }

    public @Nullable RSAPublicKey retrieveKey(PeerIdentity identity) {
        return identityCache.get(identity);
    }

    public void storeKey(PeerIdentity identity, RSAPublicKey rsaPublicKey) throws IOException {
        identityCache.put(identity, rsaPublicKey);

        File file = new File(storageFolder, identity.hexString() + ".pem");
        FileWriter fileWriter = new FileWriter(file, Charsets.UTF_8);

        try (JcaPEMWriter writer = new JcaPEMWriter(fileWriter)) {
            writer.writeObject(rsaPublicKey);
        }
    }

    private class IdentityLoader implements CacheLoader<PeerIdentity, RSAPublicKey> {
        @Override
        public @Nullable RSAPublicKey load(PeerIdentity identity) throws Exception {
            File file = new File(storageFolder, identity.hexString() + ".pem");
            if (!file.exists()) {
                return null;
            }

            try (FileReader fileReader = new FileReader(file, Charsets.UTF_8)) {
                PEMParser parser = new PEMParser(fileReader);

                var keyInfo = (SubjectPublicKeyInfo) parser.readObject();

                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                converter.setProvider(BouncyCastleProvider.PROVIDER_NAME);

                var key = (BCRSAPublicKey) converter.getPublicKey(keyInfo);

                // perform integrity check
                Preconditions.checkState(
                        identity.equals(new PeerIdentity(key)),
                        "Contents of key file " + identity.hexString() + ".pem doesn't match expected key fingerprint!"
                );

                return key;
            }
        }
    }
}