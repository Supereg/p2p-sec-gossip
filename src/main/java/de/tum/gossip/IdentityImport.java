package de.tum.gossip;

import de.tum.gossip.crypto.GossipCrypto;
import de.tum.gossip.p2p.storage.PeerIdentityStorage;
import de.tum.gossip.p2p.storage.StoredIdentity;

import java.io.File;

/**
 * Created by Andi on 12.09.22.
 */
public class IdentityImport {
    private final CLI.ImportArgs args;

    public IdentityImport(CLI.ImportArgs args) {
        this.args = args;
    }

    public void run() throws Exception {
        File storageFolder = new File(args.storageConfiguration.location);
        File inputFile = new File(args.pemLocation);

        if (!inputFile.exists()) {
            throw new Exception("The provided PEM input file doesn't exist at the location " + args.pemLocation);
        }

        var key = GossipCrypto.readHostKey(inputFile);

        var identity = key.identity;
        var entry = new StoredIdentity(args.address, args.port, key.publicKey);

        PeerIdentityStorage.unsafeStoreKey(storageFolder, identity, entry);

        System.out.println("Successfully imported identity " + identity + " into storage folder " + storageFolder.getAbsolutePath());
    }
}