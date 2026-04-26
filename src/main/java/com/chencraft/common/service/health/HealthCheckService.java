package com.chencraft.common.service.health;

import com.chencraft.common.component.AlertMessenger;
import com.chencraft.common.mongo.HealthCheckTargetRepository;
import com.chencraft.model.mongo.HealthCheckTarget;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Probes registered HealthCheckTargets and persists the latest probe outcome.
 * <p>
 * Responsibilities
 * - For each non-deleted target: issue an HTTP GET via WebClient, compare status to
 * target.expectedStatus, and update lastCheckedAt/lastSuccessAt/lastStatus accordingly.
 * - When a target has been unreachable longer than failureThresholdMinutes and an alert
 * has not yet been raised for the current outage, dispatch an email via AlertMessenger
 * and stamp alertedAt to debounce subsequent alerts until recovery.
 * - Expose per-target Prometheus metric "healthcheck.last_check_time_seconds" (Unix epoch
 * seconds of the last probe) via a Micrometer MultiGauge so dynamically added/removed
 * targets are tracked correctly.
 * <p>
 * Thread-safety: the underlying MultiGauge.register call is synchronized; reactive pipeline
 * is stateless beyond MongoDB writes (each target row uses optimistic locking via @Version).
 */
@Slf4j
@Service
public class HealthCheckService {
    private final HealthCheckTargetRepository repository;
    private final WebClient webClient;
    private final AlertMessenger alertMessenger;
    private final Clock clock;
    private final MultiGauge lastCheckGauge;

    @Autowired
    public HealthCheckService(HealthCheckTargetRepository repository,
                              WebClient webClient,
                              AlertMessenger alertMessenger,
                              Clock clock,
                              MeterRegistry meterRegistry) {
        this.repository = repository;
        this.webClient = webClient;
        this.alertMessenger = alertMessenger;
        this.clock = clock;
        this.lastCheckGauge = MultiGauge.builder("healthcheck.last_check_time_seconds")
                                        .description("Unix-epoch seconds of the most recent probe per health-check target")
                                        .register(meterRegistry);
    }

    /**
     * Probe every active target sequentially, persist the outcome, fire alerts where required,
     * then refresh Prometheus gauges. Returns a Flux of the updated records to allow tests to
     * assert on outcomes without depending on side effects.
     */
    public Flux<@NonNull HealthCheckTarget> checkAll() {
        Instant now = clock.instant();
        log.info("Starting health-check sweep at {}", now);

        return repository.findByIsDeletedFalse()
                         .flatMap(this::probeAndUpdate)
                         .collectList()
                         .doOnNext(this::publishGauges)
                         .flatMapMany(Flux::fromIterable);
    }

    private Mono<@NonNull HealthCheckTarget> probeAndUpdate(HealthCheckTarget target) {
        Instant now = clock.instant();
        // Captures per-attempt state so the final outcome reflects the LAST attempt only,
        // not stale data from an earlier successful response followed by a timeout.
        AtomicReference<Integer> lastCode = new AtomicReference<>();
        AtomicReference<String> lastError = new AtomicReference<>();

        Mono<Integer> attempt = Mono.defer(() -> {
                                        lastCode.set(null);
                                        lastError.set(null);
                                        return webClient.get()
                                                        .uri(target.getUrl())
                                                        .exchangeToMono(resp -> {
                                                            int code = resp.statusCode().value();
                                                            lastCode.set(code);
                                                            if (code == target.getExpectedStatus()) {
                                                                return Mono.just(code);
                                                            }
                                                            return Mono.error(new RuntimeException("unexpected status " + code));
                                                        });
                                    })
                                    .timeout(Duration.ofSeconds(target.getTimeoutSeconds()))
                                    .doOnError(err -> lastError.set(err.getClass()
                                                                       .getSimpleName() + ": " + err.getMessage()));

        Retry retry = Retry.fixedDelay(Math.max(0, target.getRetryAttempts()),
                                       Duration.ofSeconds(Math.max(0, target.getRetryDelaySeconds())))
                           .doBeforeRetry(sig -> log.debug("Retrying probe of {} (attempt {}/{}): {}",
                                                           target.getName(), sig.totalRetries() + 1,
                                                           target.getRetryAttempts(), sig.failure().getMessage()));

        return attempt.retryWhen(retry)
                      .map(status -> applyResult(target, now, status, null))
                      .onErrorResume(_ -> Mono.just(applyResult(target, now, lastCode.get(), lastError.get())))
                      .flatMap(this::persistAndMaybeAlert);
    }

    private HealthCheckTarget applyResult(HealthCheckTarget target, Instant now, Integer responseCode, String error) {
        target.setLastCheckedAt(now);
        target.setLastResponseCode(responseCode);
        target.setLastError(error);
        boolean ok = responseCode != null && responseCode == target.getExpectedStatus();
        if (ok) {
            target.setLastStatus(HealthCheckTarget.Status.UP);
            target.setLastSuccessAt(now);
            target.setAlertedAt(null);
        } else {
            target.setLastStatus(HealthCheckTarget.Status.DOWN);
        }
        return target;
    }

    private Mono<@NonNull HealthCheckTarget> persistAndMaybeAlert(HealthCheckTarget target) {
        if (target.getLastStatus() == HealthCheckTarget.Status.DOWN && shouldAlert(target)) {
            alertMessenger.alertHealthCheckDown(target.getName(), target.getUrl(),
                                                target.getLastSuccessAt(), target.getLastError());
            target.setAlertedAt(clock.instant());
        }
        return repository.save(target);
    }

    private boolean shouldAlert(HealthCheckTarget target) {
        if (target.getAlertedAt() != null) {
            return false;
        }
        Instant lastSuccess = target.getLastSuccessAt();
        if (lastSuccess == null) {
            // Don't alert on targets that have never been UP — likely a misconfigured URL,
            // not a server going down.
            return false;
        }
        Instant deadline = clock.instant().minus(target.getFailureThresholdMinutes(), ChronoUnit.MINUTES);
        return lastSuccess.isBefore(deadline);
    }

    private void publishGauges(List<HealthCheckTarget> targets) {
        List<MultiGauge.Row<?>> rows = new ArrayList<>(targets.size());
        for (HealthCheckTarget t : targets) {
            rows.add(MultiGauge.Row.of(
                    Tags.of("name", t.getName() == null ? "" : t.getName(),
                            "url", t.getUrl() == null ? "" : t.getUrl()),
                    t.getLastCheckedAt() == null ? 0d : t.getLastCheckedAt().getEpochSecond()));
        }
        lastCheckGauge.register(rows, true);
    }
}
