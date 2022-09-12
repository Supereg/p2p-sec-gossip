package de.tum.gossip;

import de.tum.gossip.crypto.HostKey;
import de.tum.gossip.p2p.storage.PeerIdentityStorage;
import de.tum.gossip.p2p.storage.StoredIdentity;

import java.io.File;
import java.io.IOException;

/**
 * Created by Andi on 12.09.22.
 */
public class IdentityGeneration {
    private final CLI.GenerateArgs args;
    public IdentityGeneration(CLI.GenerateArgs args) {
        this.args = args;
    }

    public void run() throws IOException {
        File storageFolder = new File(args.storageConfiguration.location);

        Integer port = args.startPort;

        for (int i = 0; i < args.amount; i++) {
            var generatedKey = HostKey.generate();

            var identity = generatedKey.identity;
            var entry = new StoredIdentity(args.defaultExpectedAddress, port, generatedKey.publicKey);

            PeerIdentityStorage.unsafeStoreKey(storageFolder, identity, entry);

            port += 1;
        }

        System.out.println("Successfully generated " + args.amount + " in folder " + storageFolder.getAbsolutePath());
    }
}