package com.chencraft.common.service.cert;

import com.chencraft.api.ApiException;
import com.chencraft.common.mongo.CertificateRepository;
import com.chencraft.model.CertificatePEM;
import com.chencraft.model.mongo.CertificateRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Primary
@Component
public class MockCertificateService extends AbstractCertificateService {
    @Autowired
    public MockCertificateService(CertificateRepository mongoRepository) {
        super(mongoRepository);
    }

    @Override
    protected CertificatePEM createCertificateAndPrivateKey(String deviceId) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(512); // small key for testing
            KeyPair dummyKeyPair = keyGen.generateKeyPair();
            Instant validUntil = Instant.now().plusSeconds(1000L * 60 * 60 * 24 * 365);

            String fakeCert = "-----BEGIN CERTIFICATE-----\n"
                    + Base64.getEncoder().encodeToString(deviceId.getBytes())
                    + "\n-----END CERTIFICATE-----";

            String fakeKey = "-----BEGIN PRIVATE KEY-----\n"
                    + Base64.getEncoder().encodeToString(dummyKeyPair.getPrivate().getEncoded())
                    + "\n-----END PRIVATE KEY-----";

            CertificateRecord record = new CertificateRecord();

            return new CertificatePEM(fakeCert, fakeKey, validUntil, record);
        } catch (NoSuchAlgorithmException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate mock keypair", e);
        }
    }
}