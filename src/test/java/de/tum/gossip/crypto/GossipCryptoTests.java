package de.tum.gossip.crypto;

import de.tum.gossip.crypto.certificates.HostKeySelfSignedX509Certificates;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by Andi on 05.07.22.
 */
public class GossipCryptoTests {
    public static File hostKeyFileFromResources() {
        var url = GossipCrypto.class.getClassLoader().getResource("hostkey.pem");
        assertNotNull(url);

        File file = assertDoesNotThrow(() -> new File(url.toURI()));
        assertTrue(file.exists());

        return file;
    }

    @Test
    void testReadHostKey() {
        var hostKey = GossipCrypto.readHostKey(hostKeyFileFromResources());
        var hashString = GossipCrypto.formatHex(hostKey.identity.rawBytes());

        assertEquals("7694cbf887929eaf7c6b8187053ef785b649e9937d406915927cbd38a484c71d", hashString);
    }

    @Test
    void testIdentitySignature() {
        var hostKey = GossipCrypto.readHostKey(hostKeyFileFromResources());

        byte[] signature = GossipCrypto.Signature.sign(hostKey.identity.rawBytes(), hostKey);

        assertEquals(512, signature.length);
        assertEquals(
                "9960475d3d62615dfca414d2704dd182ecbdb04ebb94a645eb31d4fccc71b4b8fabbdf79b7a6f0b9dcae4d6b510306" +
                        "b0500080319d120a4d4ca3fd2494a535ceb9a21400e52a98dd1085b6b6284e1711ce761eba8628a0bef3ca5d00b7a92" +
                        "ba1abd08eaa29a34d22d2df0f752a51ddd71dcc4c3beb8aab95a6acad9a6c0c4f352e59187937bcbae274a3b5657d6f" +
                        "52e4bdadf26e4be70072aa9e2c0bcbad1fa2eb24e21585ec3b271f7adf3bcc5afd0d63646bbf41424e0019494d74bb3" +
                        "8eedefab9c9bb0bc1ea5dad053883a5ac9719762d19e53dea1834c24ea1a0f166964edea662241dee457329c05287b3" +
                        "e382dcce290c4e344e618a40b23369891256b449e2ab3b68c63ba5fc7d957b1144dd22f6ec761c4eb15ad0c4a680e66" +
                        "48ad219a4f041c7b469152537206038d3dfc6ed11884270c2082f513cba30d599702d2dd2f93e3871d3bc6ba4b4aa3b" +
                        "4a3231fa08617d66445da12531d6ea2650e3fbcfc1423b83a8f199d6e69eb21b13d1249b80bf718aea3ac0a112d4225" +
                        "ef2478334689e709070cc9ba2af87f36708b33f98868490d481f1c168da32d469c079c25098a162d1640b2ce6adc4bb" +
                        "02710a9c5a2dc4bb182dc75fb4e8bbb14e6ac051b613d7af43533e34efb8dc50b6c612ef57854cbe95b2889dfcc1e1a" +
                        "0e402bc262e72dff0e9cfed71f64d629f56ee5bd3b604b318c8881294e772400b87907d41ac",
                GossipCrypto.formatHex(signature)
        );

        var validSignature = GossipCrypto.Signature.verify(hostKey.identity.rawBytes(), signature, hostKey.publicKey);

        assertTrue(validSignature);
    }

    @Test
    void testSelfSignedCertificate() {
        var hostKey = GossipCrypto.readHostKey(hostKeyFileFromResources());

        // the constructor contains a self test calling verify on itself!
        new HostKeySelfSignedX509Certificates(hostKey);
    }
}