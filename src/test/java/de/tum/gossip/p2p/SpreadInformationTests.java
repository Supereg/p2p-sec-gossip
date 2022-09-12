package de.tum.gossip.p2p;

import com.google.common.base.Charsets;
import de.tum.gossip.ConfigurationFile;
import de.tum.gossip.crypto.GossipCryptoTests;
import de.tum.gossip.mocks.MockAPIConnection;
import de.tum.gossip.mocks.MockModuleConnection;
import de.tum.gossip.p2p.util.DataType;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by Andi on 06.09.22.
 */
public class SpreadInformationTests {
    private static final ConfigurationFile MOCK_CONFIGURATION = new ConfigurationFile(GossipCryptoTests.hostKeyFileFromResources().getAbsolutePath(), 40, 20, "127.0.0.1", 8080, "127.0.0.1", 8081);

    private static final DataType TYPE_1 = new DataType(101);
    private static final DataType TYPE_2 = new DataType(102);

    private static final byte[] DATA_1 = "Hello World".getBytes(Charsets.UTF_8);

    @Test
    void testLocalInformationSpread() {
        var module = new GossipModule(MOCK_CONFIGURATION, new NioEventLoopGroup());

        var connection1 = new MockAPIConnection();
        var connection2 = new MockAPIConnection();

        // REGISTRATION
        module.registerNotification(connection1, TYPE_1);

        assertThrowsExactly(GossipException.class, () -> module.spreadInformation(connection2, 64, TYPE_1, DATA_1));
        assertThrowsExactly(GossipException.class, () -> module.spreadInformation(connection2, 64, TYPE_2, DATA_1));

        module.registerNotification(connection2, TYPE_1);

        // SPREAD
        assertDoesNotThrow(() -> module.spreadInformation(connection2, 64, TYPE_1, DATA_1));
        var packet = connection1.notificationQueue.poll();
        assertNotNull(packet);

        // VALIDATE
        // in this step we would check e.g. signatures e.t.c to validate the data!
        assertEquals(packet.dataType, TYPE_1);
        assertArrayEquals(packet.data, DATA_1);

        assertDoesNotThrow(() -> module.receiveMessageValidation(packet.messageId, true));

        // CLEANUP
        module.handleDisconnectedAPIClient(connection1);
        module.handleDisconnectedAPIClient(connection2);
    }

    @Test
    void testSimpleNetworkSpread() {
        var eventLoop = new NioEventLoopGroup();
        var moduleA = new GossipModule(MOCK_CONFIGURATION, eventLoop);
        var moduleB = new GossipModule(MOCK_CONFIGURATION, eventLoop);

        var connection1 = new MockAPIConnection();
        var connection2 = new MockAPIConnection();
        var moduleConnection = new MockModuleConnection(moduleA, moduleB);

        // REGISTRATION
        moduleA.registerNotification(connection1, TYPE_1);
        moduleB.registerNotification(connection2, TYPE_1);

        // SPREAD
        assertDoesNotThrow(() -> moduleA.spreadInformation(connection1, 64, TYPE_1, DATA_1));

        // VALIDATE
        var packet = connection2.notificationQueue.poll(); // maybe make this blocking?
        assertNotNull(packet);
        assertEquals(packet.dataType, TYPE_1);
        assertEquals(packet.data, DATA_1);
        assertDoesNotThrow(() -> moduleB.receiveMessageValidation(packet.messageId, true));

        // ----
        assertTrue(connection1.notificationQueue.isEmpty());

        // CLEANUP
        moduleConnection.teardown();
        moduleA.handleDisconnectedAPIClient(connection1);
        moduleB.handleDisconnectedAPIClient(connection2);
    }
}