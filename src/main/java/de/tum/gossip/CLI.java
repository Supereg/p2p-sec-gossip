package de.tum.gossip;

import com.beust.jcommander.*;

import java.util.Arrays;

/**
 * This is the main entry point into the application.
 * It parses options and subcommands, provides the usage menu and dispatches the sub commands.
 */
public class CLI {
    /**
     * "Global" arguments used within the whole application and all subcommands.
     */
    public static class GlobalArgs {
        @SuppressWarnings("unused") // not sure, why it thinks this is unused!
        @Parameter(names = { "-h", "--help" }, help = true, description = "Shows the help menu.")
        private boolean help;
    }

    /**
     * Reusable delegate for declaring the argument for the identity storage location.
     */
    public static class IdentityStorageConfiguration {
        @Parameter(names = { "-s", "--storage" }, description = "The location of the identity storage folder. (Default './identities').")
        public String location;
    }

    /**
     * Arguments used for running/starting the actual gossip application.
     */
    @Parameters(commandDescription = "Runs the gossip application (Default).")
    public static class RunArgs {
        @ParametersDelegate
        public IdentityStorageConfiguration storageConfiguration = new IdentityStorageConfiguration();

        @Parameter(names = { "-c", "--configuration" }, description = "Path string to the Windows INI configuration file.", required = true)
        public String configuration;

    }

    /**
     * Arguments used for the generation of exemplary peer identities.
     */
    @Parameters(commandDescription = "Generate identities for the gossip module.")
    public static class GenerateArgs {
        @ParametersDelegate
        public IdentityStorageConfiguration storageConfiguration = new IdentityStorageConfiguration();

        @Parameter(names = {"-a", "--amount"}, description = "The amount of entities to generate.")
        public Integer amount = 1;

        @Parameter(names = { "--address" }, description = "The remote address expected from all the generated identities (Default: '127.0.0.1').")
        public String defaultExpectedAddress = "127.0.0.1";

        @Parameter(names = { "--start-port" }, description = "The port to use for the identities. Increments by one for every additional identity (Default: 7000).", required = true)
        public Integer startPort = 7000;
    }

    /**
     * Arguments used for importing .pem files into our identity storage.
     */
    @Parameters(commandDescription = "Imports a .pem public key file as an identity.")
    public static class ImportArgs {
        @ParametersDelegate
        public IdentityStorageConfiguration storageConfiguration = new IdentityStorageConfiguration();

        @Parameter(names = { "-i", "--input" }, description = "The input .pem file.", required = true)
        public String pemLocation;

        @Parameter(names = { "--address" }, description = "The remote address expected from the identity.", required = true)
        public String address;
        @Parameter(names = { "--port" }, description = "The remote port expected from the identity.", required = true)
        public Integer port;
    }

    public static void main(String[] args) {
        var globals = new GlobalArgs();
        var runArgs = new RunArgs();
        var generateArgs = new GenerateArgs();
        var importArgs = new ImportArgs();

        var commander = JCommander.newBuilder()
                .addObject(globals)
                .addCommand("run", runArgs)
                .addCommand("generate", generateArgs)
                .addCommand("import", importArgs)
                .build();

        commander.setProgramName("gossip");

        try {
            try {
                commander.parse(args);
            } catch (MissingCommandException exception) {
                // workaround to provide means to use the "run" subcommand as a default command
                String[] modifiedArgs = Arrays.copyOf(new String[] { "run" }, 1 + args.length);
                System.arraycopy(args, 0, modifiedArgs, 1, args.length);
                commander.parse(modifiedArgs);
            }
        } catch (ParameterException exception) {
            System.err.println("Error: " + exception.getMessage());
            commander.usage();
            return;
        }

        if (globals.help) {
            commander.usage();
            return;
        }

        String command = commander.getParsedCommand();

        try {
            switch (command) {
                case "run" -> {
                    var app = new GossipApp(runArgs);
                    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown, "App Shutdown Hook"));
                    app.run();
                }
                case "generate" -> {
                    var identityGeneration = new IdentityGeneration(generateArgs);
                    identityGeneration.run();
                }
                case "import" -> {
                    var identityImport = new IdentityImport(importArgs);
                    identityImport.run();
                }
                default -> commander.usage();
            }
        } catch (Throwable throwable) {
            // log4j takes roughly 4s to start up, so we are loading it here dynamically!
            var logger = org.apache.logging.log4j.LogManager.getLogger(CLI.class);
            logger.error(throwable.getMessage());
            logger.debug("Failed with the following error", throwable);
        }
    }
}