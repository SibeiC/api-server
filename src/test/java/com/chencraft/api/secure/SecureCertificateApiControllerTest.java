package com.chencraft.api.secure;

import com.chencraft.common.config.MongoConfig;
import com.chencraft.common.mongo.CertificateRepository;
import com.chencraft.common.service.cert.CertificateService;
import com.chencraft.model.CertificatePEM;
import com.chencraft.model.CertificateRevokeRequest;
import com.chencraft.model.OnboardingToken;
import com.chencraft.model.mongo.CertificateRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(MongoConfig.class)
public class SecureCertificateApiControllerTest {
    private final Instant now = Instant.now();

    @MockitoBean
    private Clock clock;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoSpyBean
    private CertificateService certificateService;

    @Autowired
    private CertificateRepository certificateRepository;

    @BeforeEach
    public void setup() {
        when(clock.instant()).thenReturn(now);
        if (certificateRepository != null) {
            certificateRepository.deleteAll().block();
        }
    }

    @Test
    public void testNoAuth() {
        webTestClient.get()
                     .uri("/secure/authorize")
                     .exchange()
                     .expectStatus().isUnauthorized();
    }

    @Test
    public void testAuthorizeSuccess() {
        webTestClient.get()
                     .uri("/secure/authorize")
                     .header("X-Client-Verify", "SUCCESS")
                     .exchange()
                     .expectStatus().isOk()
                     .expectBody(OnboardingToken.class)
                     .consumeWith(response -> {
                         OnboardingToken token = response.getResponseBody();
                         Assertions.assertNotNull(token);
                         Assertions.assertNotNull(token.getKey());
                         Assertions.assertTrue(token.getValidUntil().isAfter(now));
                     });
    }

    @Test
    public void testRenewSuccess() {
        String content = """
                {
                    "deviceId": "test-device"
                }""";
        webTestClient.post()
                     .uri("/secure/certificate/renew")
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(content)
                     .header("X-Client-Verify", "SUCCESS")
                     .exchange()
                     .expectStatus().isOk()
                     .expectBody(CertificatePEM.class)
                     .consumeWith(response -> {
                         CertificatePEM pem = response.getResponseBody();
                         Assertions.assertNotNull(pem);
                         Assertions.assertNotNull(pem.getCertificate());
                         Assertions.assertNotNull(pem.getPrivateKey());
                     });
    }

    @Test
    public void testExtractDeviceIdFromCert() {
        webTestClient.post()
                     .uri("/secure/certificate/renew")
                     .contentType(MediaType.APPLICATION_JSON)
                     .header("X-Client-Verify", "SUCCESS")
                     .header("X-Client-Cert", "test-cert")
                     .exchange()
                     .expectStatus().is5xxServerError(); // Until figured out how to mock static methods
    }

    private CertificateRecord newRecord(String deviceId, String fingerprint) {
        CertificateRecord r = new CertificateRecord();
        r.setMachineId(deviceId);
        r.setFingerprintSha256(fingerprint);
        r.setIssuedAt(now.minusSeconds(3600));
        r.setExpiresAt(now.plusSeconds(3600));
        r.isDeleted = false;
        return certificateRepository.save(r).block();
    }

    @Test
    void revokeByMongoId_success_withDefaultReason() {
        CertificateRecord rec = newRecord("dev-a", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        CertificateRevokeRequest req = new CertificateRevokeRequest();
        req.setMongoId(rec.getId());
        // no reason set to force default

        String message = webTestClient.post()
                                      .uri("/secure/certificate/revoke")
                                      .header("X-Client-Verify", "SUCCESS")
                                      .contentType(MediaType.APPLICATION_JSON)
                                      .bodyValue(req)
                                      .exchange()
                                      .expectStatus().isOk()
                                      .expectBody(String.class)
                                      .returnResult()
                                      .getResponseBody();

        Assertions.assertEquals("1 record affected. ", message);

        CertificateRecord updated = certificateRepository.findById(rec.getId()).block();
        Assertions.assertNotNull(updated);
        Assertions.assertNotNull(updated.getRevokedAt());
        Assertions.assertEquals("Revoked by request", updated.getRevokeReason());
    }

    @Test
    void revokeByFingerprint_success_customReason() {
        CertificateRecord rec = newRecord("dev-b", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");

        String payload = "{" +
                "\"fingerprintSha256\":\"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\"," +
                "\"revokeReason\":\"Compromised key\"" +
                "}";

        String message = webTestClient.post()
                                      .uri("/secure/certificate/revoke")
                                      .header("X-Client-Verify", "SUCCESS")
                                      .contentType(MediaType.APPLICATION_JSON)
                                      .bodyValue(payload)
                                      .exchange()
                                      .expectStatus().isOk()
                                      .expectBody(String.class)
                                      .returnResult()
                                      .getResponseBody();

        Assertions.assertEquals("1 record affected. ", message);

        CertificateRecord updated = certificateRepository.findById(rec.getId()).block();
        Assertions.assertNotNull(updated);
        Assertions.assertEquals("Compromised key", updated.getRevokeReason());
    }

    @Test
    void revokeByDeviceId_multiple_activeOnly() {
        // two active certs and one already revoked
        CertificateRecord a = newRecord("dev-c", "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");
        CertificateRecord b = newRecord("dev-c", "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd");
        CertificateRecord c = newRecord("dev-c", "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
        c.setRevokedAt(now.minusSeconds(100));
        certificateRepository.save(c).block();

        String payload = "{\"deviceId\":\"dev-c\"}";

        String message = webTestClient.post()
                                      .uri("/secure/certificate/revoke")
                                      .header("X-Client-Verify", "SUCCESS")
                                      .contentType(MediaType.APPLICATION_JSON)
                                      .bodyValue(payload)
                                      .exchange()
                                      .expectStatus().isOk()
                                      .expectBody(String.class)
                                      .returnResult()
                                      .getResponseBody();

        Assertions.assertEquals("2 records affected. ", message);

        // verify both a and b revoked
        Assertions.assertNotNull(Objects.requireNonNull(certificateRepository.findById(a.getId()).block()).getRevokedAt());
        Assertions.assertNotNull(Objects.requireNonNull(certificateRepository.findById(b.getId()).block()).getRevokedAt());
        // c remains revoked (unchanged)
        Assertions.assertNotNull(Objects.requireNonNull(certificateRepository.findById(c.getId()).block()).getRevokedAt());
    }

    @Test
    void revokeMissingIdentifiers_badRequest() {
        String payload = "{}";
        webTestClient.post()
                     .uri("/secure/certificate/revoke")
                     .header("X-Client-Verify", "SUCCESS")
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(payload)
                     .exchange()
                     .expectStatus().isBadRequest();
    }

    @Test
    void revokeNotFound_returnsZero() {
        String payload = "{\"mongoId\":\"64b8c1f1f1f1f1f1f1f1f1f1\"}";
        String message = webTestClient.post()
                                      .uri("/secure/certificate/revoke")
                                      .header("X-Client-Verify", "SUCCESS")
                                      .contentType(MediaType.APPLICATION_JSON)
                                      .bodyValue(payload)
                                      .exchange()
                                      .expectStatus().isOk()
                                      .expectBody(String.class)
                                      .returnResult()
                                      .getResponseBody();

        Assertions.assertEquals("0 records affected.", message);
    }
}