package de.tum.gossip.p2p;

import de.tum.gossip.mocks.MockConfiguration;
import de.tum.gossip.mocks.MockHostKey;
import de.tum.gossip.mocks.MockPeerInfo;
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
public class BasicP2PTests {
    @TempDir
    public static File hostKeys;

    @Test
    void clientServerTests() throws IOException {
        var hostKey1 = MockHostKey.generate();
        var hostKey2 = MockHostKey.generate();

        // TODO workaround for key injection!
        new PeerIdentityStorage().storeKey(hostKey1.identity, hostKey1.publicKey);
        new PeerIdentityStorage().storeKey(hostKey2.identity, hostKey2.publicKey);

        var configuration1 = MockConfiguration.generate(hostKey1, hostKeys);
        var configuration2 = MockConfiguration.generate(hostKey2, hostKeys);
        var eventLoop = new NioEventLoopGroup();

        var module1 = new GossipModule(configuration1, eventLoop);
        var module2 = new GossipModule(configuration2, eventLoop);

        module1.run().syncUninterruptibly();
        module2.run().syncUninterruptibly();

        var client = module1.newClientConnection(MockPeerInfo.from(hostKey2), configuration2.p2p_address(), configuration2.p2p_port())
                .syncUninterruptibly()
                .getNow();

        var future = client.handshakeFuture();
        assertDoesNotThrow(future::sync); // sync() rethrows potential cause!
        assertTrue(future.isSuccess());

        module1.shutdown();
        module2.shutdown();

        eventLoop.shutdownGracefully().syncUninterruptibly();
    }
}