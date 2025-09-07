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

    @Override
    public void cleanUp() {
        // Mark certificates 2 months past expiration as deleted and delete them after 1 year
        Instant now = clock.instant();
        Instant softDeleteThreshold = now.minus(Duration.ofDays(60));
        Instant hardDeleteThreshold = now.minus(Duration.ofDays(365));

        // Soft-delete: mark expired and not yet deleted
        Flux<CertificateRecord> softDeleteFlow = certRepo.findAll()
                                                         .filter(rec -> rec != null
                                                                 && rec.getExpiresAt() != null
                                                                 && rec.getExpiresAt().isBefore(softDeleteThreshold)
                                                                 && !rec.isDeleted)
                                                         .flatMap(rec -> {
                                                             rec.isDeleted = true;
                                                             return certRepo.save(rec);
                                                         });

        // Hard-delete: purge records long past expiration and already marked as deleted
        Mono<Void> hardDeleteFlow = certRepo.findAll()
                                            .filter(rec -> rec != null
                                                    && rec.isDeleted
                                                    && rec.getExpiresAt() != null
                                                    && rec.getExpiresAt().isBefore(hardDeleteThreshold))
                                            .flatMap(rec -> certRepo.deleteById(rec.getId()))
                                            .then();

        // Execute asynchronously (fire-and-forget); order soft-delete then hard-delete
        fireAndForget(softDeleteFlow.then(hardDeleteFlow));
    }
}
