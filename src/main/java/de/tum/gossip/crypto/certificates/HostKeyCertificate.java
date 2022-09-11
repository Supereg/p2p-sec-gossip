package de.tum.gossip.crypto.certificates;

import de.tum.gossip.crypto.GossipCrypto;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Andi on 06.09.22.
 */
public abstract class HostKeyCertificate {
    public static final String GOSSIP_ISSUER = "GOSSIP_PEER";
    public static final String GOSSIP_SUBJECT = "GOSSIP";

    public static final long NOT_AFTER_THRESHOLD = 30*60*1000;

    /**
     * @return Returns a default NOT_BEFORE date for use within the TLS certificate. The method returns
     * the Date `now` with some random offset into the past.
     */
    protected static Date notBeforeDate() {
        int amountMinutes = GossipCrypto.SECURE_RANDOM.nextInt(0, 6);
        int amountSeconds = GossipCrypto.SECURE_RANDOM.nextInt(0, 60);

        var currentCalendar = Calendar.getInstance(TimeZone.getDefault());
        currentCalendar.add(Calendar.MINUTE, -amountMinutes);
        currentCalendar.add(Calendar.SECOND, -amountSeconds);

        return currentCalendar.getTime();
    }

    /**
     * @return Returns a default NOT_AFTER date for use within the TLS certificate. The method returns
     * the Date `npw` + 10 minutes with some random offset applied to it.
     */
    protected static Date notAfterDate() {
        int amountMinutes = GossipCrypto.SECURE_RANDOM.nextInt(0, 5);
        int amountSeconds = GossipCrypto.SECURE_RANDOM.nextInt(0, 60);

        var currentCalendar = Calendar.getInstance(TimeZone.getDefault());
        currentCalendar.add(Calendar.MINUTE, 10 + (amountMinutes - 2));
        currentCalendar.add(Calendar.SECOND, amountSeconds);

        return currentCalendar.getTime();
    }

    public abstract PrivateKey privateKey();

    public abstract X509Certificate[] certificates();

    /**
     * @return The TLS version which shall, preferably, be used with this certificate implementation.
     */
    public abstract String tlsVersionString();

    /**
     * @return The TLS cipher suites which shall, preferably, be used with this certificate implementation.
     */
    public abstract Iterable<String> tlsCipherSuites();

    /**
     * Generate a new keypair.
     * @param algorithm - Either "EC" or "RSA".
     * @param random - The SecureRandom to use for generation.
     * @param bits - THe key size.
     * @return Returns the generated KeyPair.
     */
    protected KeyPair generateKeyPair(String algorithm, SecureRandom random, int bits) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm, BouncyCastleProvider.PROVIDER_NAME);
            generator.initialize(bits, random);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            // we know that every Java implementation supports EC and provider
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve the content signer for certificate building.
     * @param signatureAlgorithm - The signature algorithm. E.g. "SHA256WithRSAEncryption" or "SHA256withECDSA".
     * @param privateKey - The private key to sign with.
     * @return Returns the Content Signer instance.
     */
    protected ContentSigner retrieveContentSigner(String signatureAlgorithm, PrivateKey privateKey) {
        try {
            // while the certificate uses elliptic curve cryptography, the hostkey is an RSA key-pair.
            return new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(privateKey);
        } catch (OperatorCreationException e) {
            throw new RuntimeException(e);
        }
    }

    protected void addX509CAOption(X509v3CertificateBuilder builder) {
        try {
            builder.addExtension(new ASN1ObjectIdentifier("2.5.29.19"), true, new BasicConstraints(true));
        } catch (CertIOException e) {
            throw new RuntimeException("Unexpected error adding CA option", e);
        }
    }
}