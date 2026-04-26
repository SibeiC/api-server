package com.chencraft.api.secure;

import com.chencraft.common.config.MongoConfig;
import com.chencraft.common.mongo.HealthCheckTargetRepository;
import com.chencraft.model.HealthCheckTargetRequest;
import com.chencraft.model.mongo.HealthCheckTarget;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Objects;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(MongoConfig.class)
public class SecureHealthCheckApiControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private HealthCheckTargetRepository repository;

    @BeforeEach
    public void cleanRepository() {
        repository.deleteAll().block();
    }

    @Test
    public void listRequiresMtls() {
        webTestClient.get()
                     .uri("/secure/healthcheck/targets")
                     .exchange()
                     .expectStatus().isUnauthorized();
    }

    @Test
    public void addThenList() {
        HealthCheckTargetRequest req = new HealthCheckTargetRequest();
        req.setName("self");
        req.setUrl("https://example.com/health");
        req.setExpectedStatus(200);

        webTestClient.post()
                     .uri("/secure/healthcheck/targets")
                     .header("X-Client-Verify", "SUCCESS")
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(req)
                     .exchange()
                     .expectStatus().isOk()
                     .expectBody(HealthCheckTarget.class)
                     .consumeWith(response -> {
                         HealthCheckTarget body = response.getResponseBody();
                         Assertions.assertNotNull(body);
                         Assertions.assertNotNull(body.getId());
                         Assertions.assertEquals("self", body.getName());
                         Assertions.assertEquals("https://example.com/health", body.getUrl());
                         Assertions.assertEquals(200, body.getExpectedStatus());
                         Assertions.assertEquals(HealthCheckTarget.Status.UNKNOWN, body.getLastStatus());
                     });

        List<HealthCheckTarget> all = webTestClient.get()
                                                   .uri("/secure/healthcheck/targets")
                                                   .header("X-Client-Verify", "SUCCESS")
                                                   .exchange()
                                                   .expectStatus().isOk()
                                                   .expectBodyList(HealthCheckTarget.class)
                                                   .returnResult()
                                                   .getResponseBody();

        Assertions.assertNotNull(all);
        Assertions.assertEquals(1, all.size());
        Assertions.assertEquals("self", all.getFirst().getName());
    }

    @Test
    public void addRejectsBlankName() {
        String payload = "{\"name\":\"\",\"url\":\"https://x\"}";
        webTestClient.post()
                     .uri("/secure/healthcheck/targets")
                     .header("X-Client-Verify", "SUCCESS")
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(payload)
                     .exchange()
                     .expectStatus().isBadRequest();
    }

    @Test
    public void deleteByIdSoftDeletes() {
        HealthCheckTarget t = new HealthCheckTarget("to-remove", "https://x");
        HealthCheckTarget saved = repository.save(t).block();
        Assertions.assertNotNull(saved);

        webTestClient.delete()
                     .uri("/secure/healthcheck/targets/" + saved.getId())
                     .header("X-Client-Verify", "SUCCESS")
                     .exchange()
                     .expectStatus().isOk()
                     .expectBody(String.class)
                     .isEqualTo("deleted");

        HealthCheckTarget after = repository.findById(saved.getId()).block();
        Assertions.assertNotNull(after);
        Assertions.assertTrue(after.isDeleted());

        // List should now exclude soft-deleted entries
        List<HealthCheckTarget> active = Objects.requireNonNullElse(
                webTestClient.get()
                             .uri("/secure/healthcheck/targets")
                             .header("X-Client-Verify", "SUCCESS")
                             .exchange()
                             .expectStatus().isOk()
                             .expectBodyList(HealthCheckTarget.class)
                             .returnResult()
                             .getResponseBody(),
                List.of());
        Assertions.assertTrue(active.isEmpty());
    }

    @Test
    public void deleteUnknownIdReturns404() {
        webTestClient.delete()
                     .uri("/secure/healthcheck/targets/64b8c1f1f1f1f1f1f1f1f1f1")
                     .header("X-Client-Verify", "SUCCESS")
                     .exchange()
                     .expectStatus().isNotFound();
    }
}
