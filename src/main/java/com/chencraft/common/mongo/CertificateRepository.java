package com.chencraft.common.mongo;

import com.chencraft.model.mongo.CertificateRecord;
import lombok.NonNull;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CertificateRepository extends ReactiveMongoRepository<@NonNull CertificateRecord, @NonNull String> {
    Flux<CertificateRecord> findByMachineIdAndIsDeletedFalse(String machineId);

    Mono<CertificateRecord> findByFingerprintSha256AndIsDeletedFalse(String fingerprint);

    Flux<CertificateRecord> findByIsDeletedFalseAndRevokedAtIsNull();
}
