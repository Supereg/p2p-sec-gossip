package de.tum.gossip.crypto.certificates;

import com.google.common.base.Preconditions;
import de.tum.gossip.crypto.GossipCrypto;
import de.tum.gossip.crypto.HostKey;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * This class is used to generate the host-key-based certificates used with TLS 1.3.
 * <p>
 * Created by Andi on 06.07.22.
 */
public class HostKeySelfSignedX509Certificates extends HostKeyCertificate {
    private final X509Certificate hostKeyCertificate;
    private final X509Certificate intermediateCertificate;
    private final PrivateKey privateKey;

    public HostKeySelfSignedX509Certificates(HostKey hostKey) {
        this(hostKey, GOSSIP_ISSUER, GOSSIP_SUBJECT, GossipCrypto.SECURE_RANDOM, 384, notBeforeDate(), notAfterDate());
    }

    public HostKeySelfSignedX509Certificates(
            HostKey hostKey,
            String issuerName,
            String subjectName,
            SecureRandom random,
            int bits,
            Date notBefore,
            Date notAfter
    ) {
        Preconditions.checkNotNull(hostKey);
        // inspired by the implementation of netty's `BouncyCastleSelfSignedCertGenerator`.

        GossipCrypto.init(); // ensure BC provider is set up

        final KeyPair keyPair = generateKeyPair("EC", random, bits);

        X500Name issuer = new X500Name("CN=" + issuerName);
        X500Name subject = new X500Name("CN=" + subjectName);

        // while the certificate uses elliptic curve cryptography, the hostkey is an RSA key-pair.
        ContentSigner signer = retrieveContentSigner("SHA256WithRSAEncryption", hostKey.privateKey);

        X509v3CertificateBuilder rootBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                new BigInteger("0"), // serial number, we choose a predictable serial number to avoid sources for fingerprinting
                notBefore,
                notAfter,
                issuer,
                hostKey.publicKey
        );
        addX509CAOption(rootBuilder);

        X509CertificateHolder rootCertificateHolder = rootBuilder.build(signer);

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer,
                new BigInteger("0"), // serial number, we choose a predictable serial number to avoid sources for fingerprinting
                notBefore,
                notAfter,
                subject,
                keyPair.getPublic() // public key associated with the certificate (=key of subject)
        );
        X509CertificateHolder certificateHolder = builder.build(signer);

        X509Certificate rootCertificate;
        X509Certificate certificate;
        try {
            var converter = new JcaX509CertificateConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);
            rootCertificate = converter.getCertificate(rootCertificateHolder);
            certificate = converter.getCertificate(certificateHolder);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }

        try {
            rootCertificate.verify(hostKey.publicKey, BouncyCastleProvider.PROVIDER_NAME);
            certificate.verify(rootCertificate.getPublicKey(), BouncyCastleProvider.PROVIDER_NAME);
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
            throw new RuntimeException(e);
        }

        this.intermediateCertificate = certificate;
        this.hostKeyCertificate = rootCertificate;
        this.privateKey = keyPair.getPrivate();
    }

    public PrivateKey privateKey() {
        return privateKey;
    }

    public X509Certificate[] certificates() {
        return new X509Certificate[] { intermediateCertificate, hostKeyCertificate };
    }

    @Override
    public String tlsVersionString() {
        return "TLSv1.3";
    }

    @Override
    public Iterable<String> tlsCipherSuites() {
        // - ECDHE: elliptic curve diffie hellman with ephemeral keys (PFS) (automatically through TLS1.3)
        // - CHACHA20_POLY1305: AEAD cipher: chacha20 encryption algorithm, Poly1305 MAC algorithm
        // - SHA256 is used for the key derivation(?)
        return List.of("TLS_CHACHA20_POLY1305_SHA256");
    }

    @Override
    public String toString() {
        return Arrays.toString(certificates());
    }
}