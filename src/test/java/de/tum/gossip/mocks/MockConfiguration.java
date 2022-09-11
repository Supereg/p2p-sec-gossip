package de.tum.gossip.mocks;

import de.tum.gossip.ConfigurationFile;
import de.tum.gossip.crypto.HostKey;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Andi on 11.09.22.
 */
public class MockConfiguration {
    private static final AtomicInteger ports = new AtomicInteger(7000);

    public static ConfigurationFile generate(HostKey hostKey, File hostKeys) {
        var file = MockHostKey.writeIntoDirectory(hostKey, hostKeys);
        return new ConfigurationFile(
                file.getPath(),
                50,
                30,
                "localhost",
                ports.getAndIncrement(),
                "localhost",
                ports.getAndIncrement()
        );
    }
}