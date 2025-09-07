package com.chencraft.common.service.cert;

import com.chencraft.common.component.Cleanable;
import com.chencraft.common.mongo.CertificateRepository;
import com.chencraft.model.mongo.CertificateRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class MTlsService implements Cleanable {
    private final CertificateRepository certRepo;

    @Autowired
    public MTlsService(CertificateRepository certRepo) {
        this.certRepo = certRepo;
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
        // TODO: Mark certificates 2 months past expiration as deleted, and subsequently delete them from the database in 1 year
    }
}
