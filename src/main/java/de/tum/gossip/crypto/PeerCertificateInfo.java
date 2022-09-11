package de.tum.gossip.crypto;

import java.security.cert.X509Certificate;
import java.util.Optional;

/**
 * Created by Andi on 11.07.22.
 */
public record PeerCertificateInfo(
        Optional<X509Certificate> intermediateCertificate,
        X509Certificate hostKeyCertificate
) {}