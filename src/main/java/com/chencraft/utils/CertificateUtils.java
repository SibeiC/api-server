package com.chencraft.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
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
}
