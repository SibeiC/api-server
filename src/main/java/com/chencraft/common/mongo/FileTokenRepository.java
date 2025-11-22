package com.chencraft.common.mongo;

import com.chencraft.model.mongo.FileToken;
import lombok.NonNull;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileTokenRepository extends ReactiveMongoRepository<@NonNull FileToken, @NonNull String> {
}
