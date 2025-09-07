package com.chencraft.api.secure;

import com.chencraft.common.service.cert.CertificateService;
import com.chencraft.model.CertificatePEM;
import com.chencraft.model.OnboardingToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Clock;
import java.time.Instant;

import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SecureCertificateApiControllerTest {
    private final Instant now = Instant.now();

    @MockitoBean
    private Clock clock;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoSpyBean
    private CertificateService certificateService;

    @BeforeEach
    public void setup() {
        when(clock.instant()).thenReturn(now);
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
}