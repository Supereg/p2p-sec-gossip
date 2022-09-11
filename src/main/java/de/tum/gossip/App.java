package de.tum.gossip;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.tum.gossip.api.GossipAPILayer;
import de.tum.gossip.p2p.GossipModule;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.*;

public class App {
    private final ConfigurationFile configurationFile;
    private final Logger logger = LogManager.getLogger(App.class);

    private final EventLoopGroup eventLoopGroup;
    private final GossipModule gossipModule;
    private final GossipAPILayer apiLayer;

    public App(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;

        // TODO support EPOLL ELG on linux based machines?
        this.eventLoopGroup = new NioEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty Server IO #%d").setDaemon(true).build());
        this.gossipModule = new GossipModule(configurationFile, eventLoopGroup);
        this.apiLayer = new GossipAPILayer(configurationFile, eventLoopGroup, gossipModule);
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

    public void shutdown() {
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
            eventLoopGroup.shutdownGracefully().syncUninterruptibly();
        } catch (Exception e) {
            logger.error("Failed to shutdown the event loop", e);
        }
    }

    public static void main(String[] args) {
        Options options = new Options();

        Option iniFileOption = new Option("c", "configuration", true, "Path string to the Windows INI configuration file!");
        iniFileOption.setRequired(true);
        options.addOption(iniFileOption);

        // TODO control log lvel by command line param!

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine commandLine;

        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException exception) {
            System.out.println(exception.getMessage());
            formatter.printHelp("gossip", options);
            return;
        }

        String iniFilePath = commandLine.getOptionValue("configuration");

        ConfigurationFile configuration;
        try {
            configuration = ConfigurationFile.readFromFile(iniFilePath);
        } catch (Exception exception) {
            System.out.println("Failed to read '" + iniFilePath + "' INI file: " + exception.getMessage());
            return;
        }

        App app = new App(configuration);

        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown, "App Shutdown Hook"));

        try {
            app.run();
        } catch (Exception e) {
            app.logger.error("Failed to start application: " + e.getMessage());
            app.logger.debug("Error occurred", e);
        }
    }
}
