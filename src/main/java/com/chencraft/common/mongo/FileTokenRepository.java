package com.chencraft.common.mongo;

import com.chencraft.model.mongo.FileToken;
import lombok.NonNull;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface FileTokenRepository extends ReactiveMongoRepository<@NonNull FileToken, @NonNull String> {
    Flux<@NonNull FileToken> findByFilenameAndIsDeletedFalse(String filename);

    Flux<@NonNull FileToken> findByTokenAndIsDeletedFalse(String token);
}
