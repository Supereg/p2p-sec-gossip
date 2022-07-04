package de.tum.gossip;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.tum.gossip.api.GossipAPILayer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.commons.cli.*;

public class App {
    private final ConfigurationFile configurationFile;

    private final EventLoopGroup eventLoopGroup;
    private final GossipAPILayer apiLayer;

    public App(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;

        // TODO support EPOLL ELG on linux based machines?
        this.eventLoopGroup = new NioEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty Server IO #%d").setDaemon(true).build());
        this.apiLayer = new GossipAPILayer(configurationFile, eventLoopGroup);
    }

    public void run() {
        this.apiLayer.run();
        // TODO blocking operation running API and P2P server/clients!

        // TODO eventLoopGroup.awaitTermination()
    }

    public static void main(String[] args) {
        Options options = new Options();

        Option iniFileOption = new Option("c", "configuration", true, "Path string to the Windows INI configuration file!");
        iniFileOption.setRequired(true);
        options.addOption(iniFileOption);

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
            configuration = new ConfigurationFile(iniFilePath);
        } catch (Exception exception) {
            System.out.println("Failed to read '" + iniFilePath + "' INI file: " + exception.getMessage());
            return;
        }

        App app = new App(configuration);
        app.run();
    }
}
