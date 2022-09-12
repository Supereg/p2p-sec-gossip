package de.tum.gossip;

import com.google.common.base.Preconditions;
import org.apache.commons.configuration2.INIConfiguration;

import java.io.File;
import java.io.FileReader;

/**
 * This java record represents state of the VoidPhone configuration file.
 * <p>
 * The configuration file is formatted in the Windows INI file format.
 *
 * @param hostkey     The hostkey of the peer; it's public-private key pair.
 *                    A 4096-bit RSA key pair store in PEM format.
 *                    The SHA256 hash of the public key serves as the peers `identity`.
 *                    It is located relative to the current working directory.
 * @param cache_size  Maximum number of data items to be held as part of the peer's knowledge base.
 *                    Older items will be removed to ensure space for newer items if the peer's
 *                    knowledge base exceeds this limit.
 * @param degree      Number of peers the current peer has to exchange information with.
 * @param p2p_address The address to bind the socket for the p2p gossip protocol.
 * @param p2p_port    The port to bind the socket for the p2p gossip protocol.
 * @param api_address The address to bind the socket for the api interface.
 * @param api_port    The port to bind the socket for the api interface.
 *
 * <h2>Example</h2>
 * An example configuration file looks like the following:
 * <pre>
 * hostkey = ./hostkey.pem
 *
 * [gossip]
 * cache_size = 40
 * degree = 20
 * p2p_address = 131.159.15.62:6001
 * api_address = 131.159.15.62:7001
 *
 * [onion]
 * hops = 2
 * </pre>
 */
public record ConfigurationFile(
        String hostkey,
        int cache_size,
        int degree,
        String p2p_address,
        int p2p_port,
        String api_address,
        int api_port
) {
    public ConfigurationFile {
        Preconditions.checkNotNull(hostkey, "`hostkey` option must be defined!");
        Preconditions.checkNotNull(p2p_address, "`gossip/p2p_address` option must be defined!");
        Preconditions.checkNotNull(api_address, "`gossip/api_address` option must be defined!");
    }

    public static ConfigurationFile readFromFile(String filePath) throws Exception {
        return readFromFile(new File(filePath));
    }

    public static ConfigurationFile readFromFile(File file) throws Exception {
        var configuration = new INIConfiguration();

        try (FileReader reader = new FileReader(file)) {
            configuration.read(reader);
        }

        var globalSection = configuration.getSection(null);
        var gossipSection = configuration.getSection("gossip");

        var hostkey = globalSection.getString("hostkey");

        var cache_size = gossipSection.getInt("cache_size", 50);
        var degree = gossipSection.getInt("degree", 30);
        var p2p_address_split = gossipSection.getString("p2p_address").split(":");
        var api_address_split = gossipSection.getString("api_address").split(":");

        Preconditions.checkState(p2p_address_split.length == 2, "Illegal format for `gossip/p2p_address`");
        Preconditions.checkState(api_address_split.length == 2, "Illegal format for `gossip/api_address`");

        var p2p_address = p2p_address_split[0];
        var p2p_port = Integer.parseInt(p2p_address_split[1]);
        var api_address = api_address_split[0];
        var api_port = Integer.parseInt(api_address_split[1]);

        return new ConfigurationFile(
                hostkey,
                cache_size,
                degree,
                p2p_address,
                p2p_port,
                api_address,
                api_port
        );
    }
}