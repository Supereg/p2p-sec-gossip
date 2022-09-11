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
 * Created by Andi on 06.07.22.
 */
public class HostKeySelfSignedX509RSACertificate extends HostKeyCertificate {
    private final X509Certificate certificate;
    private final PrivateKey privateKey;

    public HostKeySelfSignedX509RSACertificate(HostKey hostKey) {
        this(hostKey, GOSSIP_ISSUER, notBeforeDate(), notAfterDate());
    }

    public HostKeySelfSignedX509RSACertificate(
            HostKey hostKey,
            String identity,
            Date notBefore,
            Date notAfter
    ) {
        Preconditions.checkNotNull(hostKey);
        // inspired by the implementation of netty's `BouncyCastleSelfSignedCertGenerator`.

        GossipCrypto.init(); // ensure BC provider is set up

        X500Name identityName = new X500Name("CN=" + identity);

        ContentSigner signer = retrieveContentSigner("SHA256WithRSAEncryption", hostKey.privateKey);

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                identityName,
                new BigInteger("0"), // serial number, we choose a predictable serial number to avoid sources for fingerprinting
                notBefore,
                notAfter,
                identityName,
                hostKey.publicKey
        );
        addX509CAOption(builder);

        X509CertificateHolder certificateHolder = builder.build(signer);

        X509Certificate rootCertificate;
        try {
            var converter = new JcaX509CertificateConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);
            rootCertificate = converter.getCertificate(certificateHolder);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }

        try {
            rootCertificate.verify(hostKey.publicKey, BouncyCastleProvider.PROVIDER_NAME);
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
            throw new RuntimeException(e);
        }

        this.certificate = rootCertificate;
        this.privateKey = hostKey.privateKey;
    }

    public PrivateKey privateKey() {
        return privateKey;
    }

    public X509Certificate[] certificates() {
        return new X509Certificate[] { certificate };
    }

    @Override
    public String tlsVersionString() {
        // We couldn't really use TLS v1.3. We have an RSA key-pair (hostkey) as the basis as the root certificate.
        return "TLSv1.2";
    }

    @Override
    public Iterable<String> tlsCipherSuites() {
        // - Key exchange: ECDHE: elliptic curve diffie hellman with ephemeral keys (PFS)
        // - Authentication: RSA, based on our RSA certificate
        // - Encryption: CHACHA20_POLY1305 an AEAD cipher
        // - MAC: used with SHA256 (used for integrity protection)
        return List.of("TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256"); // TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
    }

    @Override
    public String toString() {
        return Arrays.toString(certificates());
    }
}