package de.tum.gossip.p2p;

import de.tum.gossip.crypto.GossipCrypto;
import de.tum.gossip.crypto.GossipCryptoTests;
import de.tum.gossip.crypto.PeerIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static de.tum.gossip.crypto.GossipCrypto.SHA256_HASH_BYTES_LENGTH;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by Andi on 06.07.22.
 */
public class PeerIdentityStorageTests {
    @TempDir
    private File storageDirectory;

    private PeerIdentityStorage storage;

    @BeforeEach
    void createStorageObject() {
        storage = new PeerIdentityStorage(storageDirectory);
    }

    @Test
    void testNonExistentRetrieval() {
        assertNull(storage.retrieveKey(new PeerIdentity(new byte[SHA256_HASH_BYTES_LENGTH])));
    }

    @Test
    void storeAndRetrieve() throws IOException {
        var hostKey = GossipCrypto.readHostKey(GossipCryptoTests.hostKeyFileFromResources());

        assertNull(storage.retrieveKey(hostKey.identity));
        storage.storeKey(hostKey.identity, hostKey.publicKey);

        var key = storage.retrieveKey(hostKey.identity);
        assertNotNull(key);
        assertEquals(hostKey.publicKey, key);
    }

    @Test
    void storeAndRetrieveFromDisk() throws IOException {
        // storage which disables caching, resulting in a forced load from disk!
        storage = new PeerIdentityStorage(storageDirectory, true);

        var hostKey = GossipCrypto.readHostKey(GossipCryptoTests.hostKeyFileFromResources());

        assertNull(storage.retrieveKey(hostKey.identity));
        storage.storeKey(hostKey.identity, hostKey.publicKey);

        var key = storage.retrieveKey(hostKey.identity);
        assertNotNull(key);
        assertEquals(hostKey.publicKey, key);
    }
}
