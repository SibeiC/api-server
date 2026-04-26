package com.chencraft.common.service.health;

import com.chencraft.common.component.AlertMessenger;
import com.chencraft.common.config.MongoConfig;
import com.chencraft.common.mongo.HealthCheckTargetRepository;
import com.chencraft.model.mongo.HealthCheckTarget;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(MongoConfig.class)
public class HealthCheckServiceTest {

    @Autowired
    private HealthCheckService service;

    @Autowired
    private HealthCheckTargetRepository repository;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockitoSpyBean
    private AlertMessenger alertMessenger;

    @MockitoSpyBean
    private Clock clock;

    private MockWebServer server;
    private Instant now;

    @BeforeEach
    public void setup() throws IOException {
        repository.deleteAll().block();
        server = new MockWebServer();
        server.start();
        now = Instant.parse("2026-04-26T10:00:00Z");
        when(clock.instant()).thenReturn(now);
    }

    @AfterEach
    public void teardown() throws IOException {
        server.shutdown();
    }

    /**
     * Builds a target with retries disabled — keeps non-retry tests fast and predictable
     * (one enqueued MockResponse per probe rather than 1 + retryAttempts).
     */
    private HealthCheckTarget noRetryTarget(String name) {
        HealthCheckTarget t = new HealthCheckTarget(name, server.url("/").toString());
        t.setRetryAttempts(0);
        t.setRetryDelaySeconds(0);
        return t;
    }

    private void save(HealthCheckTarget t) {
        HealthCheckTarget saved = repository.save(t).block();
        Assertions.assertNotNull(saved);
    }

    @Test
    public void successfulProbeMarksUp() {
        server.enqueue(new MockResponse().setResponseCode(200));

        save(noRetryTarget("up-target"));

        List<HealthCheckTarget> after = service.checkAll().collectList().block(Duration.ofSeconds(5));
        Assertions.assertNotNull(after);
        Assertions.assertEquals(1, after.size());
        HealthCheckTarget result = after.getFirst();
        Assertions.assertEquals(HealthCheckTarget.Status.UP, result.getLastStatus());
        Assertions.assertEquals(now, result.getLastSuccessAt());
        Assertions.assertEquals(now, result.getLastCheckedAt());
        Assertions.assertEquals(200, result.getLastResponseCode());
        verify(alertMessenger, never()).alertHealthCheckDown(any(), any(), any(), any());
    }

    @Test
    public void failingProbeMarksDownButDoesNotAlertOnFreshTarget() {
        server.enqueue(new MockResponse().setResponseCode(500));

        save(noRetryTarget("never-up"));

        List<HealthCheckTarget> after = service.checkAll().collectList().block(Duration.ofSeconds(5));
        Assertions.assertNotNull(after);
        HealthCheckTarget result = after.getFirst();
        Assertions.assertEquals(HealthCheckTarget.Status.DOWN, result.getLastStatus());
        Assertions.assertNull(result.getLastSuccessAt());
        Assertions.assertEquals(500, result.getLastResponseCode());
        // never been UP, so alert is suppressed
        verify(alertMessenger, never()).alertHealthCheckDown(any(), any(), any(), any());
    }

    @Test
    public void prolongedOutageTriggersAlertOnceThenDebounces() {
        // Two queued failures so we can probe twice (no retries)
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(500));

        // Target that was up an hour ago — well past the 5 minute threshold
        HealthCheckTarget t = noRetryTarget("flaky");
        t.setLastSuccessAt(now.minusSeconds(3600));
        t.setFailureThresholdMinutes(5);
        save(t);

        service.checkAll().collectList().block(Duration.ofSeconds(5));
        verify(alertMessenger, times(1))
                .alertHealthCheckDown(eq("flaky"), any(), eq(now.minusSeconds(3600)), any());

        // Second sweep — alertedAt now stamped, so no new alert
        service.checkAll().collectList().block(Duration.ofSeconds(5));
        verify(alertMessenger, times(1)).alertHealthCheckDown(any(), any(), any(), any());
    }

    @Test
    public void recoveryClearsAlertedAtSoNextOutageAlertsAgain() {
        HealthCheckTarget t = noRetryTarget("recovering");
        t.setLastSuccessAt(now.minusSeconds(3600));
        t.setAlertedAt(now.minusSeconds(60));
        t.setFailureThresholdMinutes(5);
        save(t);

        // Recovery probe — 200
        server.enqueue(new MockResponse().setResponseCode(200));
        List<HealthCheckTarget> after = service.checkAll().collectList().block(Duration.ofSeconds(5));
        Assertions.assertNotNull(after);
        HealthCheckTarget refreshed = after.getFirst();
        Assertions.assertEquals(HealthCheckTarget.Status.UP, refreshed.getLastStatus());
        Assertions.assertNull(refreshed.getAlertedAt());

        // Now another outage — should alert again because alertedAt was cleared
        Instant later = now.plusSeconds(3600);
        when(clock.instant()).thenReturn(later);
        server.enqueue(new MockResponse().setResponseCode(500));
        service.checkAll().collectList().block(Duration.ofSeconds(5));

        // lastSuccessAt is `now` (set during recovery probe), and `later - now` = 3600s > 5min threshold
        verify(alertMessenger, times(1))
                .alertHealthCheckDown(eq("recovering"), any(), eq(now), any());
    }

    @Test
    public void prometheusGaugeRegisteredPerTarget() {
        server.enqueue(new MockResponse().setResponseCode(200));
        save(noRetryTarget("metered"));

        service.checkAll().collectList().block(Duration.ofSeconds(5));

        // The MultiGauge registers each row under the same metric name with distinct tags.
        List<Gauge> gauges = meterRegistry.find("healthcheck.last_check_time_seconds")
                                          .gauges()
                                          .stream()
                                          .toList();
        Assertions.assertFalse(gauges.isEmpty(), "Expected gauge to be registered");
        Gauge gauge = gauges.stream()
                            .filter(g -> "metered".equals(g.getId().getTag("name")))
                            .findFirst()
                            .orElseThrow();
        Assertions.assertEquals(now.getEpochSecond(), gauge.value(), 0.5);
        // Sanity check that the metric type is what Prometheus will scrape
        Assertions.assertEquals(Meter.Type.GAUGE, gauge.getId().getType());
    }

    @Test
    public void retriesRecoverFromFlakyEndpoint() {
        // Two failures followed by a success — with retryAttempts=2 the third attempt wins
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(200));

        HealthCheckTarget t = new HealthCheckTarget("flappy", server.url("/").toString());
        t.setRetryAttempts(2);
        t.setRetryDelaySeconds(0); // no inter-retry sleep in tests
        save(t);

        List<HealthCheckTarget> after = service.checkAll().collectList().block(Duration.ofSeconds(10));
        Assertions.assertNotNull(after);
        HealthCheckTarget result = after.getFirst();
        Assertions.assertEquals(HealthCheckTarget.Status.UP, result.getLastStatus(),
                                "after retry-recovery the target should be UP");
        Assertions.assertEquals(200, result.getLastResponseCode());
        Assertions.assertEquals(3, server.getRequestCount(),
                                "expected 1 initial probe + 2 retries");
    }

    @Test
    public void retriesExhaustedMarkDown() {
        // 1 initial + 2 retries = 3 failures, all 503
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(503));

        HealthCheckTarget t = new HealthCheckTarget("dead", server.url("/").toString());
        t.setRetryAttempts(2);
        t.setRetryDelaySeconds(0);
        save(t);

        List<HealthCheckTarget> after = service.checkAll().collectList().block(Duration.ofSeconds(10));
        Assertions.assertNotNull(after);
        HealthCheckTarget result = after.getFirst();
        Assertions.assertEquals(HealthCheckTarget.Status.DOWN, result.getLastStatus());
        Assertions.assertEquals(503, result.getLastResponseCode(),
                                "lastResponseCode should reflect the LAST attempt's status");
        Assertions.assertEquals(3, server.getRequestCount());
    }
}
