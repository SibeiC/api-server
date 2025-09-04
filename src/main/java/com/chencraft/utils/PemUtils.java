package com.chencraft.utils;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class PemUtils {

    /**
     * Pass in certificates in increasing order by depth. i.e. leaf -> intermediate -> root
     *
     * @param certs certificates to be converted to PEM format
     * @return PEM format certificates chain
     */
    public static String toPem(X509Certificate... certs) throws IOException {
        try (StringWriter stringWriter = new StringWriter();
             JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
            for (X509Certificate cert : certs) {
                pemWriter.writeObject(cert);
                pemWriter.flush();
            }
            return stringWriter.toString();
        }
    }

    public static String toPem(PrivateKey privateKey) throws IOException {
        try (StringWriter stringWriter = new StringWriter();
             JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
            pemWriter.writeObject(privateKey);
            pemWriter.flush();
            return stringWriter.toString();
        }
    }
}
