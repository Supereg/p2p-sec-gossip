package de.tum.gossip.p2p.storage;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.tum.gossip.crypto.GossipCrypto;
import de.tum.gossip.crypto.PeerIdentity;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static de.tum.gossip.crypto.GossipCrypto.SHA256_HASH_BYTES_LENGTH;
import static de.tum.gossip.crypto.GossipCrypto.logger;

/**
 * Local storage of known identities.
 * <p>
 * Created by Andi on 06.07.22.
 */
public class PeerIdentityStorage implements Iterable<Map.Entry<PeerIdentity, StoredIdentity>> {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(StoredIdentity.class, new StoredIdentity.Serializer())
            .create();

    private final File storageFolder;
    private final LoadingCache<PeerIdentity, StoredIdentity> identityCache;
    private final ReadWriteLock storageLock = new ReentrantReadWriteLock();

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

    private static void ensureStorageFolderCreated(File storageFolder) {
        if (!storageFolder.exists()) {
            var result = storageFolder.mkdirs();
            Preconditions.checkState(result, "Failed to create PeerIdentityStorage folder at " + storageFolder.getAbsolutePath());
        }
    }

    /**
     * Loads all identities from disk. You can use the iterator afterwards to get access to the stored identities!
     */
    public void loadAll() {
        if (!storageFolder.exists()) {
            return;
        }

        var files = storageFolder.listFiles(PeerIdentityStorage::identityFileNameFilter);
        if (files == null) {
            return;
        }

        for (var file: files) {
            var parts = file.getName().split("\\.");
            Preconditions.checkState(parts.length == 2);
            var identity = new PeerIdentity(parts[0]);

            // will result in the cache loader to get called!
            identityCache.get(identity);
        }
    }

    private static boolean identityFileNameFilter(File dir, String name) {
        var parts = name.split("\\.");
        boolean structuralMatch = parts.length == 2 && parts[0].length() == 2 * SHA256_HASH_BYTES_LENGTH && parts[1].equalsIgnoreCase("json");
        if (!structuralMatch) {
            logger.debug("Found illegal file name in storage directory: " + name + " at " + dir.getAbsolutePath());
            return false;
        }
        try {
            GossipCrypto.fromHex(parts[0]);
            return true;
        } catch (NumberFormatException exception) {
            logger.debug("Found illegal json file name in storage directory: " + name + " at " + dir.getAbsolutePath());
            return false;
        }
    }

    public @Nullable StoredIdentity retrieveKey(PeerIdentity identity) {
        this.storageLock.readLock().lock();
        try {
            return identityCache.get(identity);
        } finally {
            this.storageLock.readLock().unlock();
        }
    }

    public void storeKey(PeerIdentity identity, StoredIdentity entry) throws IOException {
        this.storageLock.writeLock().lock();
        try {
            identityCache.put(identity, entry);

            // it's safe in this place, we just reuse the routine here!
            unsafeStoreKey(storageFolder, identity, entry);
        } finally {
            this.storageLock.writeLock().unlock();
        }
    }

    public static void unsafeStoreKey(File storageFolder, PeerIdentity identity, StoredIdentity entry) throws IOException {
        ensureStorageFolderCreated(storageFolder);

        File file = new File(storageFolder, identity.hexString() + ".json");
        try (FileWriter fileWriter = new FileWriter(file, Charsets.UTF_8)) {
            gson.toJson(entry, fileWriter);
        }
    }

    @NotNull
    @Override
    public Iterator<Map.Entry<PeerIdentity, StoredIdentity>> iterator() {
        return identityCache.asMap().entrySet().iterator();
    }

    private class IdentityLoader implements CacheLoader<PeerIdentity, StoredIdentity> {
        @Override
        public @Nullable StoredIdentity load(PeerIdentity identity) throws Exception {
            File file = new File(storageFolder, identity.hexString() + ".json");
            if (!file.exists()) {
                return null;
            }

            try (FileReader fileReader = new FileReader(file, Charsets.UTF_8)) {
                var entry = gson.fromJson(fileReader, StoredIdentity.class);

                // perform integrity check
                Preconditions.checkState(
                        identity.equals(entry.peerIdentity()),
                        "Contents of key file " + identity.hexString() + ".pem doesn't match expected key fingerprint!"
                );

                return entry;
            }
        }
    }
}