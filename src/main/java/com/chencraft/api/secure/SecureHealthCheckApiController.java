package com.chencraft.api.secure;

import com.chencraft.common.mongo.HealthCheckTargetRepository;
import com.chencraft.model.HealthCheckTargetRequest;
import com.chencraft.model.mongo.HealthCheckTarget;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@SecurityScheme(type = SecuritySchemeType.MUTUALTLS, name = "mTLS")
public class SecureHealthCheckApiController implements SecureHealthCheckApi {
    private final HealthCheckTargetRepository repository;
    private final long defaultFailureThresholdMinutes;
    private final int defaultRetryAttempts;
    private final long defaultRetryDelaySeconds;

    @Autowired
    public SecureHealthCheckApiController(HealthCheckTargetRepository repository,
                                          @Value("${app.healthcheck.failure-threshold-minutes:60}") long defaultFailureThresholdMinutes,
                                          @Value("${app.healthcheck.retry-attempts:2}") int defaultRetryAttempts,
                                          @Value("${app.healthcheck.retry-delay-seconds:2}") long defaultRetryDelaySeconds) {
        this.repository = repository;
        this.defaultFailureThresholdMinutes = defaultFailureThresholdMinutes;
        this.defaultRetryAttempts = defaultRetryAttempts;
        this.defaultRetryDelaySeconds = defaultRetryDelaySeconds;
    }

    @Override
    public Flux<@NonNull HealthCheckTarget> listTargets() {
        return repository.findByIsDeletedFalse();
    }

    @Override
    public Mono<@NonNull ResponseEntity<@NonNull HealthCheckTarget>> addTarget(HealthCheckTargetRequest request) {
        HealthCheckTarget target = new HealthCheckTarget(request.getName(), request.getUrl());
        target.setFailureThresholdMinutes(defaultFailureThresholdMinutes);
        target.setRetryAttempts(defaultRetryAttempts);
        target.setRetryDelaySeconds(defaultRetryDelaySeconds);
        if (request.getExpectedStatus() != null) {
            target.setExpectedStatus(request.getExpectedStatus());
        }
        if (request.getTimeoutSeconds() != null) {
            target.setTimeoutSeconds(request.getTimeoutSeconds());
        }
        if (request.getFailureThresholdMinutes() != null) {
            target.setFailureThresholdMinutes(request.getFailureThresholdMinutes());
        }
        if (request.getRetryAttempts() != null) {
            target.setRetryAttempts(request.getRetryAttempts());
        }
        if (request.getRetryDelaySeconds() != null) {
            target.setRetryDelaySeconds(request.getRetryDelaySeconds());
        }
        return repository.save(target).map(ResponseEntity::ok);
    }

    @Override
    public Mono<@NonNull ResponseEntity<@NonNull String>> deleteTarget(String id) {
        return repository.findById(id)
                         .flatMap(existing -> {
                             if (existing.isDeleted()) {
                                 return Mono.just(ResponseEntity.ok("already deleted"));
                             }
                             existing.setDeleted(true);
                             return repository.save(existing).thenReturn(ResponseEntity.ok("deleted"));
                         })
                         .defaultIfEmpty(ResponseEntity.status(404).body("not found"));
    }
}
