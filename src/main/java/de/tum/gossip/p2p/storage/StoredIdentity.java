package de.tum.gossip.p2p.storage;

import com.google.gson.*;
import de.tum.gossip.crypto.PeerIdentity;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.security.interfaces.RSAPublicKey;

/**
 * Instance of a locally stored gossip peer.
 *
 * @param lastSeenHostname - The last known ip address of the peer. Used for address pinning and for establishing a connection.
 * @param lastSeenPort - The last known port of the peer. Used for establishing a connection.
 * @param publicKey - The public key of the host key of the peer, serving as the peer's identity.
 *
 * Created by Andi on 12.09.22.
 */
public record StoredIdentity(
        String lastSeenHostname,
        Integer lastSeenPort,
        RSAPublicKey publicKey
) {
    public StoredIdentity(RSAPublicKey publicKey) {
        this(null, null, publicKey);
    }

    public PeerIdentity peerIdentity() {
        return new PeerIdentity(publicKey);
    }

    public boolean hasAddressInformation() {
        return lastSeenHostname != null && lastSeenPort != null;
    }

    public static class Serializer implements JsonSerializer<StoredIdentity>, JsonDeserializer<StoredIdentity> {
        @Override
        public StoredIdentity deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject object = json.getAsJsonObject();

                String lastSeenHostname = null;
                Integer lastSeenPort = null;
                RSAPublicKey publicKey;

                if (object.has("lastSeenHostname")) {
                    lastSeenHostname = object.get("lastSeenHostname").getAsString();
                }
                if (object.has("lastSeenPort")) {
                    lastSeenPort = object.get("lastSeenPort").getAsInt();
                }

                StringReader stringReader = new StringReader(object.get("publicKeyPEM").getAsString());
                try (PEMParser parser = new PEMParser(stringReader)) {
                    var keyInfo = (SubjectPublicKeyInfo) parser.readObject();

                    JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                    converter.setProvider(BouncyCastleProvider.PROVIDER_NAME);

                    publicKey = (BCRSAPublicKey) converter.getPublicKey(keyInfo);
                }

                return new StoredIdentity(lastSeenHostname, lastSeenPort, publicKey);
            } catch (IllegalStateException | IOException exception) {
                throw new JsonParseException("Failed to parse storage entry", exception);
            }
        }

        @Override
        public JsonElement serialize(StoredIdentity entry, Type type, JsonSerializationContext context) {
            JsonObject object = new JsonObject();

            if (entry.lastSeenHostname != null) {
                object.addProperty("lastSeenHostname", entry.lastSeenHostname);
            }
            if (entry.lastSeenPort != null) {
                object.addProperty("lastSeenPort", entry.lastSeenPort);
            }

            var stringWriter = new StringWriter();
            try (JcaPEMWriter writer = new JcaPEMWriter(stringWriter)) {
                writer.writeObject(entry.publicKey);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            object.addProperty("publicKeyPEM", stringWriter.toString());

            return object;
        }
    }
}