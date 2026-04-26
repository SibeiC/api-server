package com.chencraft.api;

import com.chencraft.common.config.MongoConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MongoConfig.class)
@AutoConfigureWebTestClient
public class HealthCheckApiControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void selfHealthCheckReturnsOk() {
        webTestClient.get()
                     .uri("/healthcheck")
                     .exchange()
                     .expectStatus().isOk()
                     .expectBody(String.class)
                     .isEqualTo("ok");
    }

    @Test
    public void selfHealthCheckIsPublic() {
        // No mTLS header — the public self-ping must still be reachable.
        webTestClient.get()
                     .uri("/healthcheck")
                     .exchange()
                     .expectStatus().isOk();
    }
}
