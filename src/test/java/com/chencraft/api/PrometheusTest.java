package com.chencraft.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Test to ensure prometheus and micrometer dependency stays in pom.xml
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@AutoConfigureWebTestClient
public class PrometheusTest {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testPrometheusEndpointExists() {
        webTestClient.get()
                     .uri("/actuator/prometheus")
                     .exchange()
                     .expectStatus().isOk();
    }
}
