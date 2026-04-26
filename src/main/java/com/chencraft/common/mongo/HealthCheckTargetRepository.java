package com.chencraft.common.mongo;

import com.chencraft.model.mongo.HealthCheckTarget;
import lombok.NonNull;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface HealthCheckTargetRepository
        extends ReactiveMongoRepository<@NonNull HealthCheckTarget, @NonNull String> {

    Flux<@NonNull HealthCheckTarget> findByIsDeletedFalse();
}
