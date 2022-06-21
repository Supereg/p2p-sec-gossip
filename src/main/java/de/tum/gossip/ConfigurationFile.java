package de.tum.gossip;

import org.ini4j.Ini;
import org.ini4j.IniPreferences;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * Created by Andi on 20.06.22.
 */
public class ConfigurationFile {
    // global options
    public final String hostkey;

    // gossip options
    public final int cache_size;
    public final int degree;
    // public final String bootstrapper; // TODO what is this used for?
    public final String p2p_address;
    public final String api_address;

    public ConfigurationFile(String filePath) throws Exception {
        this(new File(filePath));
    }
    public ConfigurationFile(File file) throws Exception {
        Preferences preferences = new IniPreferences(new Ini(file));

        Preferences globalNode = preferences.node("global");
        hostkey = globalNode.get("hostkey", "");

        Preferences gossipNode = preferences.node("gossip");
        cache_size = gossipNode.getInt("cache_size", 50);
        degree = gossipNode.getInt("degree", 30);
        // bootstrapper = gossipNode.get("bootstrapper", "");
        p2p_address = gossipNode.get("p2p_address", "");
        api_address = gossipNode.get("api_address", "");

        if (hostkey.equals("")) {
            throw new Exception("`hostkey` option must be defined!");
        }

        /*
        if (bootstrapper.equals("")) {
            throw new Exception("`gossip/bootstrapper` option must be defined!");
        }
        */

        if (p2p_address.equals("")) {
            throw new Exception("`gossip/p2p_address` option must be defined!");
        }

        if (api_address.equals("")) {
            throw new Exception("`gossip/api_address` option must be defined!");
        }
    }
}