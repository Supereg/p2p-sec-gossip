package de.tum.gossip.crypto;

import de.tum.gossip.crypto.certificates.HostKeyCertificate;
import de.tum.gossip.crypto.certificates.HostKeySelfSignedX509Certificates;
import de.tum.gossip.crypto.certificates.HostKeySelfSignedX509RSACertificate;
import de.tum.gossip.p2p.GossipPeerInfo;
import io.netty.util.internal.EmptyArrays;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Trust manager, verifying the integrity of the provided certificates within the TLS handshake.
 *
 */
public class SelfSignedCertifyingTrustManager implements X509TrustManager {
    public final GossipPeerInfo expectedIdentity;

    private enum CallOrigin {
        SERVER,
        CLIENT,
    }

    private final Class<? extends HostKeyCertificate> type;

    public SelfSignedCertifyingTrustManager(Class<? extends HostKeyCertificate> type) {
        this(type, null);
    }

    public SelfSignedCertifyingTrustManager(Class<? extends HostKeyCertificate> type, GossipPeerInfo expectedIdentity) {
        this.expectedIdentity = expectedIdentity;
        this.type = type;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
        checkTrusted(x509Certificates, authType, CallOrigin.CLIENT);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
        checkTrusted(x509Certificates, authType, CallOrigin.SERVER);
    }

    private void assertCertificateCount(X509Certificate[] certificates, int size) throws CertificateException {
        if (certificates.length != size) {
            throw new CertificateException("Illegal size of certificate chain 1: " + certificates.length);
        }
    }

    private void checkTrusted(X509Certificate[] certificates, String authType, CallOrigin origin) throws CertificateException {
        if (type.equals(HostKeySelfSignedX509RSACertificate.class)) {
            assertCertificateCount(certificates, 1);
        } else if (type.equals(HostKeySelfSignedX509Certificates.class)) {
            assertCertificateCount(certificates, 2);
        } else {
            throw new RuntimeException("Unknown certificate type!");
        }

        var rootCertificate = certificates[certificates.length - 1];

        try {
            rootCertificate.verify(rootCertificate.getPublicKey(), BouncyCastleProvider.PROVIDER_NAME);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
            throw new CertificateException("Failed to assert self signed root certificate", e);
        }

        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
        } catch (KeyStoreException | NoSuchAlgorithmException | IOException e) {
            throw new CertificateException("Failed to get KeyStore", e);
        }

        try {
            // root certificate will always be the gossip issue, irrelevant of the type!
            keyStore.setCertificateEntry(HostKeySelfSignedX509Certificates.GOSSIP_ISSUER, rootCertificate);
        } catch (KeyStoreException e) {
            throw new CertificateException("Failed to add root certificate", e);
        }

        X509TrustManager trustManager;
        try {
            var factory = TrustManagerFactory.getInstance("PKIX");
            factory.init(keyStore);
            trustManager = (X509TrustManager) factory.getTrustManagers()[0];
        } catch (Exception e) {
            throw new CertificateException("Failed to init TrustManagerFactory", e);
        }

        switch (origin) {
            case CLIENT -> trustManager.checkClientTrusted(certificates, authType);
            case SERVER -> trustManager.checkServerTrusted(certificates, authType);
        }

        long currentMillis = System.currentTimeMillis();
        for (var certificate: certificates) {
            // check general time validity
            certificate.checkValidity();

            // we require short-lived certificates in our protocol!
            if (certificate.getNotAfter().getTime() - currentMillis > HostKeyCertificate.NOT_AFTER_THRESHOLD) {
                throw new CertificateException("Provided certificate is considered to have a too long not-after date: " + certificate.getSubjectX500Principal().getName() + "!");
            }
        }

        if (expectedIdentity != null) {
            try {
                rootCertificate.verify(expectedIdentity.publicKey(), BouncyCastleProvider.PROVIDER_NAME);
            } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
                throw new CertificateException("Failed to assert root identity", e);
            }
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return EmptyArrays.EMPTY_X509_CERTIFICATES;
    }
}