package de.tum.gossip.crypto;

import java.security.cert.X509Certificate;
import java.util.Optional;

/**
 * Describes the certificate information of a remote peer, after a completed TLS handshake.
 *
 * @param intermediateCertificate - Optional intermediate certificate (present when using {@link de.tum.gossip.crypto.certificates.HostKeySelfSignedX509Certificates}).
 * @param hostKeyCertificate - The hostkey-based certificate. CA certificate.
 *
 * Created by Andi on 11.07.22.
 */
public record PeerCertificateInfo(
        Optional<X509Certificate> intermediateCertificate,
        X509Certificate hostKeyCertificate
) {}