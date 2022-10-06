package de.tum.gossip;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.tum.gossip.api.GossipAPILayer;
import de.tum.gossip.p2p.GossipModule;
import de.tum.gossip.p2p.storage.PeerIdentityStorage;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class GossipApp {
    public final Logger logger = LogManager.getLogger(GossipApp.class);

    private final EventLoopGroup eventLoopGroup;
    public final GossipModule gossipModule;
    public final GossipAPILayer apiLayer;

    public GossipApp(CLI.RunArgs args) throws Exception {
        this(argsToConfiguration(args), args.storageConfiguration.location);
    }

    private static ConfigurationFile argsToConfiguration(CLI.RunArgs args) throws Exception {
        String iniFilePath = args.configuration;

        ConfigurationFile configuration;
        try {
            configuration = ConfigurationFile.readFromFile(iniFilePath);
        } catch (Exception exception) {
            throw new Exception("Failed to read '" + iniFilePath + "' INI file: " + exception.getMessage(), exception);
        }
        return configuration;
    }

    public GossipApp(ConfigurationFile configuration) {
        this(configuration, new PeerIdentityStorage());
    }

    public GossipApp(ConfigurationFile configuration, String location) {
        this(configuration, new PeerIdentityStorage(new File(location)));
    }

    public GossipApp(ConfigurationFile configuration, PeerIdentityStorage identityStorage) {
        // TODO support EPOLL ELG on linux based machines?
        this.eventLoopGroup = new NioEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty Server IO #%d").build());
        this.gossipModule = new GossipModule(configuration, eventLoopGroup, identityStorage);
        this.apiLayer = new GossipAPILayer(configuration, eventLoopGroup, gossipModule);
    }

    public void run() throws Exception {
        try {
            this.gossipModule.run().syncUninterruptibly();
        } catch (Exception e) {
            throw new Exception("Failed to run gossip module: " + e.getMessage(), e);
        }
        try {
            this.apiLayer.run().syncUninterruptibly();
        } catch (Exception e) {
            throw new Exception("Failed to run api layer: " + e.getMessage(), e);
        }
    }

    /**
     * Shuts down the Gossip App.
     * Never call this within the EventLoop!
     */
    public void shutdown() {
        // when shutdown is called, run() might not have completed successfully!

        try {
            this.apiLayer.shutdown().syncUninterruptibly();
        } catch (Exception e) {
            logger.error("Shutdown of API layer completed erroneously", e);
        }

        try {
            this.gossipModule.shutdown().syncUninterruptibly();
        } catch (Exception e) {
            logger.error("Shutdown of Gossip module completed erroneously", e);
        }

        try {
            boolean inEventLoop = false;
            for (var eventLoop: eventLoopGroup) {
                if (eventLoop.inEventLoop()) {
                    inEventLoop = true;
                    break;
                }
            }

            if (inEventLoop) {
                logger.error("Tried to shutdown Gossip App from within the event loop!");
                eventLoopGroup.shutdownGracefully(); // we can't sync now, otherwise we would create a deadlock
            } else {
                eventLoopGroup.shutdownGracefully().syncUninterruptibly();
            }
        } catch (Exception e) {
            logger.error("Failed to shutdown the event loop", e);
        }
    }
}
