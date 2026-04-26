package com.chencraft.common.task;

import com.chencraft.common.service.executor.TaskExecutor;
import com.chencraft.common.service.health.HealthCheckService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static com.chencraft.utils.PublisherUtils.fireAndForget;

/**
 * Periodically triggers HealthCheckService.checkAll(). Cadence is configured via
 * app.healthcheck.interval-seconds (default 60s); the scan reuses the shared TaskExecutor.
 */
@Slf4j
@Component
public class HealthCheckScheduler {
    private final HealthCheckService healthCheckService;
    private final TaskExecutor taskExecutor;
    private final long intervalSeconds;

    @Autowired
    public HealthCheckScheduler(HealthCheckService healthCheckService,
                                TaskExecutor taskExecutor,
                                @Value("${app.healthcheck.interval-seconds:60}") long intervalSeconds) {
        this.healthCheckService = healthCheckService;
        this.taskExecutor = taskExecutor;
        this.intervalSeconds = intervalSeconds;
    }

    @PostConstruct
    public void init() {
        log.info("Scheduling health-check sweep every {} seconds", intervalSeconds);
        taskExecutor.scheduleAtFixedRate(this::runSweep, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void runSweep() {
        try {
            fireAndForget(healthCheckService.checkAll());
        } catch (Exception e) {
            log.error("Failed to start health-check sweep", e);
        }
    }
}
