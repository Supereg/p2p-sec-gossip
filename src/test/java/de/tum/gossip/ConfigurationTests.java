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

        ConfigurationFile configuration = assertDoesNotThrow(() -> ConfigurationFile.readFromFile(new File(url.toURI())));

        assertEquals("./hostkey.pem", configuration.hostkey());
        assertEquals(40, configuration.cache_size());
        assertEquals(20, configuration.degree());
        assertEquals("131.159.15.62", configuration.p2p_address());
        assertEquals(6001, configuration.p2p_port());
        assertEquals("131.159.15.62", configuration.api_address());
        assertEquals(7001, configuration.api_port());
    }
}