package com.chencraft.api;

import com.chencraft.common.component.AuthorizationTokenStorage;
import com.chencraft.model.CertificatePEM;
import com.chencraft.model.OnboardingToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.boot.webtestclient.AutoConfigureWebTestClient;
import java.time.Clock;
import java.time.Instant;

import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class CertificateApiControllerTest {
    private Instant now = Instant.now();

    private OnboardingToken token;

    @MockitoBean
    private Clock clock;

    @Autowired
    private AuthorizationTokenStorage authTokenStorage;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    public void setup() {
        when(clock.instant()).thenReturn(now);
        token = authTokenStorage.createToken();

        now = now.plusSeconds(60);
        when(clock.instant()).thenReturn(now);
    }

    @Test
    public void testNoAuthToken() {
        webTestClient.get()
                     .uri("/certificate/issue")
                     .exchange()
                     .expectStatus().isBadRequest();
    }

    @Test
    public void testIncorrectAuthToken() {
        webTestClient.get()
                     .uri("/certificate/issue?token=" + "MALICIOUS_TOKEN" + "&deviceId=" + "test")
                     .exchange()
                     .expectStatus().isUnauthorized();
    }

    @Test
    public void testExpiredAuthToken() {
        now = now.plusSeconds(86400); // 1 day
        when(clock.instant()).thenReturn(now);

        webTestClient.get()
                     .uri("/certificate/issue?token=" + token.getKey() + "&deviceId=" + "test")
                     .exchange()
                     .expectStatus().isUnauthorized();
    }

    @Test
    public void testSuccess() {
        webTestClient.get()
                     .uri("/certificate/issue?token=" + token.getKey() + "&deviceId=" + "test")
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
    public void testRepeatedToken() {
        testSuccess();

        webTestClient.get()
                     .uri("/certificate/issue?token=" + token.getKey() + "&deviceId=" + "test")
                     .exchange()
                     .expectStatus().isUnauthorized();
    }
}
