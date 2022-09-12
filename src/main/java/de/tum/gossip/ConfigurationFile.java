package de.tum.gossip;

import com.google.common.base.Preconditions;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * This java record represents state of the VoidPhone configuration file.
 *
 * The configuration file is formatted in the Windows INI file format.
 *
 * @param hostkey     The hostkey of the peer; it's public-private key pair.
 *                    A 4096-bit RSA key pair store in PEM format.
 *                    The SHA256 hash of the public key serves as the peers `identity`.
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
 * [global]
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
        Preconditions.checkState(!hostkey.equals(""), "`hostkey` option must be defined!");
        Preconditions.checkState(!p2p_address.equals(""), "`gossip/p2p_address` option must be defined!");
        Preconditions.checkState(!api_address.equals(""), "`gossip/api_address` option must be defined!");
    }

    public static ConfigurationFile readFromFile(String filePath) throws Exception {
        return readFromFile(new File(filePath));
    }

    public static ConfigurationFile readFromFile(File file) throws Exception {
        Preferences preferences = new IniPreferences(new Ini(file));

        // TODO hostkey might not be located in the node "global"!!!
        Preferences globalNode = preferences.node("global");
        Preferences gossipNode = preferences.node("gossip");

        // TODO relative to the configuration file?
        var hostkey = globalNode.get("hostkey", "");

        var cache_size = gossipNode.getInt("cache_size", 50);
        var degree = gossipNode.getInt("degree", 30);
        var p2p_address_split = gossipNode.get("p2p_address", "").split(":");
        var api_address_split = gossipNode.get("api_address", "").split(":");

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