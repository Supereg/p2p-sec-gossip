package de.tum.gossip.p2p;

import de.tum.gossip.crypto.HostKey;
import de.tum.gossip.mocks.MockConfiguration;
import de.tum.gossip.mocks.MockPeerInfo;
import de.tum.gossip.p2p.storage.PeerIdentityStorage;
import de.tum.gossip.p2p.storage.StoredIdentity;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by Andi on 06.07.22.
 */
public class GossipModuleTests {
    @TempDir
    public static File hostKeys;
    @TempDir
    public static File identityStorage;

    @Test
    void simpleClientServerTests() throws IOException {
        var hostKey1 = HostKey.generate();
        var hostKey2 = HostKey.generate();

        var configuration1 = MockConfiguration.generate(hostKey1, hostKeys);
        var configuration2 = MockConfiguration.generate(hostKey2, hostKeys);

        PeerIdentityStorage.unsafeStoreKey(identityStorage, hostKey1.identity, new StoredIdentity(hostKey1.publicKey));
        PeerIdentityStorage.unsafeStoreKey(identityStorage, hostKey2.identity, new StoredIdentity(hostKey2.publicKey));

        var eventLoop = new NioEventLoopGroup();
        var module1 = new GossipModule(configuration1, eventLoop, new PeerIdentityStorage(identityStorage));
        var module2 = new GossipModule(configuration2, eventLoop, new PeerIdentityStorage(identityStorage));

        module1.run().syncUninterruptibly();
        module2.run().syncUninterruptibly();

        var client = module1.newClientContext(MockPeerInfo.from(hostKey2), configuration2.p2p_address(), configuration2.p2p_port());

        client.connect()
                .syncUninterruptibly();

        var future = client.handshakeFuture();
        assertDoesNotThrow(future::sync); // sync() rethrows potential cause!
        assertTrue(future.isSuccess());

        module1.shutdown();
        module2.shutdown();

        eventLoop.shutdownGracefully().syncUninterruptibly();
    }
}