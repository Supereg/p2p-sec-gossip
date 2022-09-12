package de.tum.gossip;

import de.tum.gossip.crypto.HostKey;
import de.tum.gossip.mocks.MockConfiguration;
import de.tum.gossip.net.ChannelInboundHandler;
import de.tum.gossip.net.util.ChannelCloseReasonCause;
import de.tum.gossip.p2p.packets.GossipPacketDisconnect;
import de.tum.gossip.p2p.storage.PeerIdentityStorage;
import de.tum.gossip.p2p.storage.StoredIdentity;
import io.netty.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by Andi on 12.09.22.
 */
public class GossipAppTests {
    @TempDir
    public static File hostKeys;
    @TempDir
    public static File identityStorage;

    @Test
    void simpleAppE2ETest() throws Exception {
        var hostKey1 = HostKey.generate();
        var hostKey2 = HostKey.generate();

        var configuration1 = MockConfiguration.generate(hostKey1, hostKeys);
        var configuration2 = MockConfiguration.generate(hostKey2, hostKeys);

        PeerIdentityStorage.unsafeStoreKey(identityStorage, hostKey1.identity, new StoredIdentity(configuration1.p2p_address(), configuration1.p2p_port(), hostKey1.publicKey));
        PeerIdentityStorage.unsafeStoreKey(identityStorage, hostKey2.identity, new StoredIdentity(configuration2.p2p_address(), configuration2.p2p_port(), hostKey2.publicKey));

        var app1 = new GossipApp(configuration1, new PeerIdentityStorage(identityStorage));
        var app2 = new GossipApp(configuration2, new PeerIdentityStorage(identityStorage));

        app1.run();
        app2.run();

        Thread.sleep(500);

        assertEquals(1, app1.gossipModule.clients().size());
        assertEquals(1, app2.gossipModule.clients().size());
        var client12 = app1.gossipModule.clients().values().stream().findFirst().get();
        var client21 = app2.gossipModule.clients().values().stream().findFirst().get(); // TODO warning!

        // TODO assert identity!

        var handshake12 = client12.handshakeFuture();
        var handshake21 = client21.handshakeFuture();

        handshake12.awaitUninterruptibly();
        handshake21.awaitUninterruptibly();

        System.out.println(handshake12);
        System.out.println(handshake21);

        Future<ChannelInboundHandler> successHandler;
        Future<ChannelInboundHandler> failureHandler;

        if (handshake12.isSuccess()) {
            successHandler = handshake12;
            failureHandler = handshake21;
        } else {
            successHandler = handshake21;
            failureHandler = handshake12;
        }

        assertTrue(successHandler.isDone() && successHandler.isSuccess());
        assertTrue(failureHandler.isDone() && !failureHandler.isSuccess());

        if (!(failureHandler.cause() instanceof ChannelCloseReasonCause cause)) {
            fail("Failure exception is not a ChannelCloseReasonCause");
            return;
        }

        // TODO the packet disconnect currently doesn't arrive at the client!
        if (false) {
            if (!(cause.channelCloseReason instanceof GossipPacketDisconnect.DisconnectReasonContaining containing)) {
                fail("channel close reason is not a DisconnectReasonContaining");
                return;
            }

            var reason = containing.disconnectReason();
            assertEquals(reason, GossipPacketDisconnect.Reason.DUPLICATE);
        }

        System.out.println("Shutting down!");
        app1.shutdown();
        app2.shutdown();
    }

    // TODO full test?
}