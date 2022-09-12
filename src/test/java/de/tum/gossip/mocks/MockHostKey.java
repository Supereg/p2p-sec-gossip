package de.tum.gossip.mocks;

import com.google.common.base.Preconditions;
import de.tum.gossip.crypto.GossipCrypto;
import de.tum.gossip.crypto.HostKey;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Andi on 11.09.22.
 */
public class MockHostKey {
    private static final AtomicInteger fileIndex = new AtomicInteger(0);

    public static File writeIntoDirectory(HostKey hostKey, File parent) {
        Preconditions.checkState(parent.isDirectory());

        var file = new File(parent, "hostkey-" + fileIndex.incrementAndGet() + ".pem");
        GossipCrypto.writeHostKey(hostKey, file);
        return file;
    }
}