/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package de.tum.gossip;

import org.apache.commons.cli.*;

public class App {
    private final ConfigurationFile configurationFile;

    public App(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;
    }

    public void run() {
        // TODO blocking operation running API and P2P server/clients!
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
