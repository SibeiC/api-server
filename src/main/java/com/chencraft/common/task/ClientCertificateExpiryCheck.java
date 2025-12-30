package com.chencraft.common.task;

import com.chencraft.common.component.AlertMessenger;
import com.chencraft.common.mongo.CertificateRepository;
import com.chencraft.common.service.executor.TaskExecutor;
import com.chencraft.model.mongo.CertificateRecord;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static com.chencraft.utils.PublisherUtils.fireAndForget;

/**
 * Periodic background task that scans client mTLS certificate records to detect certificates that are
 * about to expire or have recently expired but were not explicitly revoked.
 * <p>
 * Responsibilities
 * - Queries MongoDB (reactive) for active, non-deleted client certificates.
 * - Filters for certificates expiring within the next 7 days or those that expired within the last 2 days.
 * - Logs an entry for each affected certificate and sends an alert email via AlertMessenger.
 * - Runs once at application startup and then on a fixed daily schedule using TaskExecutor.
 * <p>
 * Thread-safety
 * - This component is stateless and designed to be used as a Spring singleton. Scheduling is delegated
 * to TaskExecutor; the reactive pipeline is executed asynchronously (fire-and-forget) and does not
 * retain a mutable shared state.
 * <p>
 * External I/O
 * - Reads certificate records from MongoDB through CertificateRepository.
 * - Sends email notifications via AlertMessenger/MailService.
 * <p>
 * Configuration
 * - Depends on injected Clock for deterministic and testable timing.
 * - Uses TaskExecutor for scheduling. No additional properties are required for this task itself.
 */
@Slf4j
@Component
public class ClientCertificateExpiryCheck {
    private final AlertMessenger alertMessenger;
    private final TaskExecutor taskExecutor;
    private final CertificateRepository certRepo;
    private final Clock clock;

    /**
     * Creates a new ClientCertificateExpiryCheck.
     *
     * @param alertMessenger component used to send alert emails when expired, non-revoked certificates are found; must not be null
     * @param taskExecutor   executor used for scheduling periodic scans; must not be null
     * @param certRepo       reactive repository for reading certificate records from MongoDB; must not be null
     * @param clock          clock providing the current time; injected for determinism in tests; must not be null
     */
    @Autowired
    public ClientCertificateExpiryCheck(AlertMessenger alertMessenger,
                                        TaskExecutor taskExecutor,
                                        CertificateRepository certRepo,
                                        Clock clock) {
        this.alertMessenger = alertMessenger;
        this.taskExecutor = taskExecutor;
        this.certRepo = certRepo;
        this.clock = clock;
    }

    /**
     * Schedules the client certificate expiry scan to run immediately at startup and then once every day.
     * <p>
     * Side effects
     * - Registers a fixed-rate task with TaskExecutor. The actual scan work is performed asynchronously
     * using a reactive pipeline and PublisherUtils.fireAndForget.
     */
    @PostConstruct
    public void init() {
        this.taskExecutor.scheduleAtFixedRate(this::clientCheck, 0, 1, TimeUnit.DAYS);
    }

    private void clientCheck() {
        Instant now = clock.instant();
        log.info("Starting client certificate expiry scan at {}", now);

        Instant sevenDaysLater = now.plus(7, ChronoUnit.DAYS);
        Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

        Flux<@NonNull CertificateRecord> candidates = certRepo.findByIsDeletedFalseAndRevokedAtIsNull()
                                                               .filter(rec -> rec.getExpiresAt() != null &&
                                                                       rec.getExpiresAt().isBefore(sevenDaysLater) &&
                                                                       rec.getExpiresAt().isAfter(twoDaysAgo));

        fireAndForget(
                candidates
                        .doOnNext(rec -> {
                            boolean isExpired = rec.getExpiresAt().isBefore(now);
                            LocalDate validUntil = LocalDate.ofInstant(rec.getExpiresAt(), ZoneId.systemDefault());

                            if (isExpired) {
                                log.warn("Client certificate expired: machineId={}, fingerprint={}, expiredAt={}",
                                        rec.getMachineId(), rec.getFingerprintSha256(), validUntil);
                            } else {
                                log.info("Client certificate expiring soon: machineId={}, fingerprint={}, expiresAt={}",
                                        rec.getMachineId(), rec.getFingerprintSha256(), validUntil);
                            }
                            alertMessenger.alertCertificateExpiry(rec.getMachineId(), validUntil, isExpired, false);
                        })
                        .doOnComplete(() -> log.info("Completed client certificate expiry scan"))
        );
    }
}
