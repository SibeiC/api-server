package com.chencraft.utils;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HexFormat;

public class CertificateUtils {
    public static String computeSha256Fingerprint(X509Certificate cert) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = cert.getEncoded();
            byte[] hash = digest.digest(encoded);
            return HexFormat.of().formatHex(hash).toUpperCase();
        } catch (CertificateEncodingException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String computeSha256Fingerprint(String rawCert) {
        X509Certificate cert = parseCertificate(rawCert);
        return computeSha256Fingerprint(cert);
    }

    public static String extractCNSubject(String rawCert) {
        X509Certificate cert = parseCertificate(rawCert);
        return new X500Name(cert.getSubjectX500Principal().getName())
                .getRDNs(BCStyle.CN)[0].getFirst().getValue().toString();
    }

    private static X509Certificate parseCertificate(String rawCert) {
        try {
            rawCert = rawCert.replace("-----BEGIN CERTIFICATE-----", "")
                             .replace("-----END CERTIFICATE-----", "")
                             .replaceAll("\\s+", "");
            byte[] der = java.util.Base64.getDecoder().decode(rawCert);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(der));
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }
}
