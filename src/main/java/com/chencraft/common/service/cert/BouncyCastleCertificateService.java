package com.chencraft.common.service.cert;

import com.chencraft.api.ApiException;
import com.chencraft.common.component.AlertMessenger;
import com.chencraft.common.component.AppConfig;
import com.chencraft.common.service.executor.TaskExecutor;
import com.chencraft.model.CertificatePEM;
import com.chencraft.model.mongo.CertificateRecord;
import com.chencraft.utils.PemUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;

@Lazy
@Slf4j
@Service
public class BouncyCastleCertificateService extends AbstractCertificateService {
    private static final int DEFAULT_VALIDITY_DAYS = 60;
    private static final String BC_PROVIDER = "BC";
    private static final String KEY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private final AlertMessenger messenger;
    private final TaskExecutor taskExecutor;
    private final AppConfig appConfig;
    private final X509Certificate caCert;
    private final PrivateKey caPrivateKey;

    @Autowired
    public BouncyCastleCertificateService(AlertMessenger messenger,
                                          TaskExecutor taskExecutor,
                                          AppConfig appConfig,
                                          MTlsService mtlsService,
                                          @Value("${app.tls.keystore}") String keystorePath,
                                          @Value("${app.tls.keystore-password}") String keystorePassword,
                                          @Value("${app.tls.keystore-alias}") String alias) throws Exception {
        super(mtlsService);

        this.messenger = messenger;
        this.taskExecutor = taskExecutor;
        this.appConfig = appConfig;

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            ks.load(fis, keystorePassword.toCharArray());
        }

        this.caCert = (X509Certificate) ks.getCertificate(alias);
        this.caPrivateKey = (PrivateKey) ks.getKey(alias, keystorePassword.toCharArray());

        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        if (caCert == null || caPrivateKey == null) {
            throw new IllegalStateException("Could not load CA cert or private key from keystore");
        }
        log.info("Loaded CA certificate: {}", caCert.getSubjectX500Principal().getName());
    }

    @PostConstruct
    public void init() {
        this.taskExecutor.scheduleAtFixedRate(this::certificateCheck, 0, 1, java.util.concurrent.TimeUnit.DAYS);
    }

    private void certificateCheck() {
        // If the certificate is expiring in 30 days, shoot the reminder
        Instant monthFromNow = Instant.now().plusSeconds(DEFAULT_VALIDITY_DAYS * 24L * 60 * 60);
        boolean shouldNotify = this.caCert.getNotAfter().toInstant().isBefore(monthFromNow);
        String deviceName = new X500Name(this.caCert.getSubjectX500Principal().getName())
                .getRDNs(BCStyle.CN)[0].getFirst().getValue().toString();

        if (shouldNotify) {
            LocalDate validity = LocalDate.ofInstant(this.caCert.getNotAfter().toInstant(), ZonedDateTime.now().getZone());
            if (appConfig.isDev()) {
                throw new RuntimeException("Certificate is expiring on " + validity + ", please renew before continuing.");
            } else {
                messenger.alertCertificateExpiring(deviceName, validity);
            }
        }
    }

    @Override
    protected CertificatePEM createCertificateAndPrivateKey(String deviceId) {
        Instant validUntil = Instant.now().plusSeconds(DEFAULT_VALIDITY_DAYS * 24L * 60 * 60);
        KeyPair clientKey = generateClientKeyPair();
        X509Certificate clientCert = issueClientCertificate(clientKey, deviceId, validUntil);
        CertificateRecord record = new CertificateRecord(clientCert, deviceId);

        try {
            String certPem = PemUtils.toPem(clientCert, this.caCert);
            String keyPem = PemUtils.toPem(clientKey.getPrivate());
            return new CertificatePEM(certPem, keyPem, validUntil, record);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to convert certificate to PEM", e);
        }
    }

    private X509Certificate issueClientCertificate(KeyPair clientKeyPair, String clientCn, Instant validUntil) {
        // Update X500Name
        X500Name rootCertIssuer = new X500Name(RFC4519Style.INSTANCE, this.caCert.getSubjectX500Principal().getName());
        String serverCn = rootCertIssuer.getRDNs(BCStyle.CN)[0].getFirst().getValue().toString();
        String updatedDnString = rootCertIssuer.toString()
                                               .replace("CN=" + serverCn, "CN=" + clientCn)
                                               .replace("cn=" + serverCn, "cn=" + clientCn);
        X500Name clientCertSubject = new X500Name(updatedDnString);

        // Set up certificate validity dates
        Date notBefore = new Date();
        Date notAfter = Date.from(validUntil);

        // Generating a CSR (Certificate Signing Request)
        BigInteger issuedCertSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()));
        log.info("Issuing certificate for device {} with subject {}", clientCn, clientCertSubject);

        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(clientCertSubject, clientKeyPair.getPublic());
        JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(BC_PROVIDER);

        // Sign the new KeyPair with the root cert Private Key
        ContentSigner csrContentSigner;
        try {
            csrContentSigner = csrBuilder.build(this.caPrivateKey);
        } catch (OperatorCreationException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create CSR signer", e);
        }
        PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner);

        // Use the Signed KeyPair and CSR to generate an issued Certificate
        // Here serial number is randomly generated. In general, CAs use
        // a sequence to generate Serial number and avoid collisions
        X509v3CertificateBuilder issuedCertBuilder = new X509v3CertificateBuilder(rootCertIssuer, issuedCertSerialNum, notBefore, notAfter, csr.getSubject(), csr.getSubjectPublicKeyInfo());

        JcaX509ExtensionUtils issuedCertExtUtils;
        try {
            issuedCertExtUtils = new JcaX509ExtensionUtils();
        } catch (NoSuchAlgorithmException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create extension utils", e);
        }

        try {
            // Add extensions
            issuedCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            issuedCertBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
            issuedCertBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}));

            // Add Issuer cert identifier
            issuedCertBuilder.addExtension(Extension.authorityKeyIdentifier, false, issuedCertExtUtils.createAuthorityKeyIdentifier(this.caCert));
            issuedCertBuilder.addExtension(Extension.subjectKeyIdentifier, false, issuedCertExtUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo()));
        } catch (CertIOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to add extensions", e);
        } catch (CertificateEncodingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to encode certificate", e);
        }

        X509CertificateHolder issuedCertHolder = issuedCertBuilder.build(csrContentSigner);
        try {
            return new JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(issuedCertHolder);
        } catch (CertificateException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to convert issued certificate to X509Certificate", e);
        }
    }

    private KeyPair generateClientKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM, BC_PROVIDER);
            keyGen.initialize(4096);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "RSA not supported", e);
        } catch (NoSuchProviderException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "BC provider not found", e);
        }
    }
}
