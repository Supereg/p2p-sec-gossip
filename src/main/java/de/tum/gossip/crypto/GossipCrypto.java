package de.tum.gossip.crypto;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import de.tum.gossip.net.ChannelInboundHandler;
import de.tum.gossip.net.ConnectionInitializer;
import de.tum.gossip.p2p.GossipPeerInfo;
import io.netty.handler.ssl.SslHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.io.pem.PemObject;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;

/**
 * This helper class contains useful primitives for all crypto operations happening inside this module.
 */
public class GossipCrypto {
    public static final int SHA256_HASH_BYTES_LENGTH = 32;
    public static final int RSA_SIGNATURE_BYTES_LENGTH = 512;
    public static final Logger logger = LogManager.getLogger(GossipCrypto.class);

    public static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static {
        // this works only on Java >= 9, however we use records which impose a much higher language limit anyway.
        Security.setProperty("crypto.policy", "unlimited"); // required to unlock AES-256

        var bouncyCastle = new BouncyCastleProvider();
        System.out.println("Running crypto with bouncy castle version " + bouncyCastle);
        Security.addProvider(bouncyCastle);
    }

    /**
     * Calling this empty method will ensure that our `static` block above is called!
     * Components relying on the initialization above might call this method to ensure this class is loaded.
     */
    public static void init() {}

    public static HostKey readHostKey(File file) {
        try (FileReader reader = new FileReader(file)) {
            PEMParser parser = new PEMParser(reader);

            PEMKeyPair keyPair;
            try {
                keyPair = (PEMKeyPair) parser.readObject();
            } catch (ClassCastException e) { // read unexpected PEM object!
                logger.error("Failed to parse hostkey", e);
                throw new RuntimeException(e);
            }

            if (keyPair == null) {
                throw new RuntimeException("No objects left in PEM file: " + file.getAbsolutePath());
            }

            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            converter.setProvider(BouncyCastleProvider.PROVIDER_NAME);

            RSAPublicKey publicKey = (BCRSAPublicKey) converter.getPublicKey(keyPair.getPublicKeyInfo());
            RSAPrivateKey privateKey = (BCRSAPrivateKey) converter.getPrivateKey(keyPair.getPrivateKeyInfo());

            return new HostKey(publicKey, privateKey);
        } catch (IOException e) {
            // TODO failed to read file!
            throw new RuntimeException(e);
        }
    }

    public static void writeHostKey(HostKey hostKey, File file) {
        if (!file.exists()) {
            try {
                Preconditions.checkState(file.createNewFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try (FileWriter fileWriter = new FileWriter(file)) {
            JcaPEMWriter pemWriter = new JcaPEMWriter(fileWriter);


            PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(hostKey.privateKey.getEncoded());
            var pemObject = new PemObject(PEMParser.TYPE_RSA_PRIVATE_KEY, privateKeyInfo.parsePrivateKey().toASN1Primitive().getEncoded());
            pemWriter.writeObject(pemObject);

            pemWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method generates the identity of a given RSA public key.
     * The SHA256 of the public key, is considered the identity of a peer.
     * <p>
     * This method results in the same output as you would get when executing
     * `openssl rsa -in publickey.pem -pubin -outform der | openssl dgst -sha256`.
     *
     * @param key - The {@link RSAPublicKey} to generate the identity for.
     * @return Returns the SHA256 hash in the form of a byte array.
     */
    public static byte[] publicKeyIdentity(RSAPublicKey key)  {
        byte[] keyBytes = key.getEncoded();

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256", BouncyCastleProvider.PROVIDER_NAME);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            // TODO something went horribly wrong!
            throw new RuntimeException(e);
        }

        digest.update(keyBytes);
        return digest.digest();
    }

    public static String formatHex(byte[] hash) {
        return String.format("%0" + (hash.length*2) + "x", new BigInteger(1, hash));
    }

    public static byte[] combineBytes(byte[]... bytesBytes) {
        int size = 0;
        for (byte[] bytes : bytesBytes) {
            size += bytes.length;
        }

        var stream = new ByteArrayOutputStream(size);
        for (byte[] bytes : bytesBytes) {
            stream.writeBytes(bytes);
        }

        return stream.toByteArray();
    }

    public static SslHandler getSslHandler(ChannelInboundHandler channel) {
        return (SslHandler) channel.getHandle().pipeline().get(ConnectionInitializer.Ids.TLS_HANDLER);
    }

    public static PeerCertificateInfo peerCertificateFromTLSSession(ChannelInboundHandler channel) throws SSLPeerUnverifiedException, IllegalStateException {
        var handler = getSslHandler(channel);
        SSLSession session = handler.engine().getSession();
        // https://docs.oracle.com/javase/8/docs/api/javax/net/ssl/SSLEngine.html#getSession--
        // docs: 'Until the initial handshake has completed, this method returns a session object which reports an invalid cipher suite of "SSL_NULL_WITH_NULL_NULL".'
        Preconditions.checkState(!session.getCipherSuite().equals("SSL_NULL_WITH_NULL_NULL"));

        Certificate[] certificates = session.getPeerCertificates();

        for (var instance: certificates) {
            Preconditions.checkState(instance instanceof X509Certificate);
        }


        if (certificates.length == 2) {
            return new PeerCertificateInfo(Optional.of((X509Certificate) certificates[0]), (X509Certificate) certificates[1]);
        } else if (certificates.length == 1) {
            return new PeerCertificateInfo(null, (X509Certificate) certificates[0]);
        } else {
            throw new IllegalStateException("Encountered illegal certificate chain length!");
        }
    }

    public static class Signature {
        public static byte[] signChallenge(HostKey hostKey, byte[] challenge) {
            var bytes = GossipCrypto.combineBytes(
                    "HS-CHALLENGE".getBytes(Charsets.UTF_8),
                    challenge,
                    hostKey.identity.rawBytes()
            );
            return sign(bytes, hostKey);
        }

        public static boolean verifyChallenge(GossipPeerInfo peerInfo, byte[] signature, byte[] challenge) {
            // TODO coupling with the gossip module!
            var bytes = GossipCrypto.combineBytes(
                    "HS-CHALLENGE".getBytes(Charsets.UTF_8),
                    challenge,
                    peerInfo.identity().rawBytes()
            );
            return verify(bytes, signature, peerInfo.publicKey());
        }

        public static byte[] sign(byte[] data, HostKey hostkey) {
            java.security.Signature signer;
            try {
                signer = java.security.Signature.getInstance("SHA512WithRSA", BouncyCastleProvider.PROVIDER_NAME);
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                // TODO sever error case!
                throw new RuntimeException(e);
            }

            try {
                signer.initSign(hostkey.privateKey);
            } catch (InvalidKeyException e) {
                // won't happen as long as our key is properly encoded!
                throw new RuntimeException(e);
            }

            try {
                signer.update(data);
                return signer.sign();
            } catch (SignatureException e) {
                // won't ever throw, as we properly initialize the signer by calling `initSign` above!
                throw new RuntimeException(e);
            }
        }

        public static boolean verify(byte[] data, byte[] signature, RSAPublicKey publicKey) {
            java.security.Signature signer;
            try {
                signer = java.security.Signature.getInstance("SHA512WithRSA", BouncyCastleProvider.PROVIDER_NAME);
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                // TODO sever error case!
                throw new RuntimeException(e);
            }

            try {
                signer.initVerify(publicKey);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            }

            try {
                signer.update(data);
                return signer.verify(signature);
            } catch (SignatureException e) {
                // won't ever throw, as we properly initialize the signer by calling `initVerify` above!
                throw new RuntimeException(e);
            }
        }
    }
}