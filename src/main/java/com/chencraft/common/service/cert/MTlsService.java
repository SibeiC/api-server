package com.chencraft.common.service.cert;

import com.chencraft.common.component.Cleanable;
import com.chencraft.common.mongo.CertificateRepository;
import com.chencraft.model.mongo.CertificateRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static com.chencraft.utils.PublisherUtils.fireAndForget;

@Service
public class MTlsService implements Cleanable {
    private final CertificateRepository certRepo;
    private final Clock clock;

    @Autowired
    public MTlsService(CertificateRepository certRepo, Clock clock) {
        this.certRepo = certRepo;
        this.clock = clock;
    }

    @Cacheable(value = "certificatesByFingerprint", key = "#fingerprint")
    public Mono<CertificateRecord> findByFingerprint(String fingerprint) {
        return certRepo.findByFingerprintSha256AndIsDeletedFalse(fingerprint);
    }

    @CacheEvict(value = "certificatesByFingerprint", allEntries = true)
    public Mono<CertificateRecord> insertNewRecord(CertificateRecord record) {
        return certRepo.save(record)
                       .flatMap(savedRecord ->
                                        certRepo.findByMachineIdAndIsDeletedFalse(savedRecord.getMachineId())
                                                .filter(old -> !old.getFingerprintSha256().equals(savedRecord.getFingerprintSha256()))
                                                .flatMap(old -> {
                                                    // Revoke old cert
                                                    old.setRevokedAt(record.getIssuedAt());
                                                    old.setRevokeReason("Superseded by new certificate");
                                                    return certRepo.save(old);
                                                })
                                                .then(Mono.just(savedRecord)));
    }

    /**
     * Revoke a certificate by MongoDB id.
     *
     * @return Mono<Boolean> true if a record was found and revoked; false otherwise.
     */
    @CacheEvict(value = "certificatesByFingerprint", allEntries = true)
    public Mono<Boolean> revokeById(String id, String reason) {
        Instant now = clock.instant();
        return certRepo.findById(id)
                       .filter(record -> !record.isDeleted)
                       .flatMap(record -> applyRevocation(record, now, reason).thenReturn(true))
                       .defaultIfEmpty(false);
    }

    /**
     * Revoke a certificate by SHA-256 fingerprint.
     *
     * @return Mono<Boolean> true if a record was found and revoked; false otherwise.
     */
    @CacheEvict(value = "certificatesByFingerprint", key = "#fingerprint")
    public Mono<Boolean> revokeByFingerprint(String fingerprint, String reason) {
        Instant now = clock.instant();
        return certRepo.findByFingerprintSha256AndIsDeletedFalse(fingerprint)
                       .flatMap(record -> applyRevocation(record, now, reason).thenReturn(true))
                       .defaultIfEmpty(false);
    }

    /**
     * Revoke all active certificates for a device (machineId).
     *
     * @return Mono<Long> count of revoked records.
     */
    @CacheEvict(value = "certificatesByFingerprint", allEntries = true)
    public Mono<Long> revokeByDeviceId(String deviceId, String reason) {
        Instant now = clock.instant();
        return certRepo.findByMachineIdAndIsDeletedFalse(deviceId)
                       .filter(record -> record.getRevokedAt() == null)
                       .flatMap(record -> applyRevocation(record, now, reason))
                       .count();
    }

    private Mono<CertificateRecord> applyRevocation(CertificateRecord record, Instant now, String reason) {
        record.setRevokedAt(now);
        if (reason == null || reason.isBlank()) {
            record.setRevokeReason("Revoked by request");
        } else {
            record.setRevokeReason(reason);
        }
        return certRepo.save(record);
    }

    @Override
    public void cleanUp() {
        // Mark certificates 2 months past expiration as deleted and delete them after 1 year
        Instant now = clock.instant();
        Instant softDeleteThreshold = now.minus(Duration.ofDays(60));
        Instant hardDeleteThreshold = now.minus(Duration.ofDays(365));

        // Soft-delete: mark expired and not yet deleted
        Flux<CertificateRecord> softDeleteFlow = certRepo.findAll()
                                                         .filter(rec -> rec.getExpiresAt() != null && rec.getExpiresAt().isBefore(softDeleteThreshold) && !rec.isDeleted)
                                                         .flatMap(rec -> {
                                                             rec.isDeleted = true;
                                                             return certRepo.save(rec);
                                                         });

        // Hard-delete: purge records long past expiration and already marked as deleted
        Mono<Void> hardDeleteFlow = certRepo.findAll()
                                            .filter(rec -> rec.isDeleted && rec.getExpiresAt() != null && rec.getExpiresAt().isBefore(hardDeleteThreshold))
                                            .flatMap(rec -> certRepo.deleteById(rec.getId()))
                                            .then();

        // Execute asynchronously (fire-and-forget); order soft-delete then hard-delete
        fireAndForget(softDeleteFlow.then(hardDeleteFlow));
    }
}
