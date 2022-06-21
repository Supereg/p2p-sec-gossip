package de.tum.gossip;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by Andi on 21.06.22.
 */
public class ConfigurationTests {
    @Test
    void testConfigurationFileParsing() {
        var url = ConfigurationTests.class.getClassLoader().getResource("test-config.ini");
        assertNotNull(url);

        ConfigurationFile configuration = assertDoesNotThrow(() -> new ConfigurationFile(new File(url.toURI())));

        assertEquals(configuration.hostkey, "/hostkey.pem");
        assertEquals(configuration.cache_size, 40);
        assertEquals(configuration.degree, 20);
        assertEquals(configuration.p2p_address, "131.159.15.62:6001");
        assertEquals(configuration.api_address, "131.159.15.62:7001");
    }
}