package com.chencraft.common.service.file;

import com.chencraft.api.FileApi;
import com.chencraft.common.component.Cleanable;
import com.chencraft.common.mongo.FileTokenRepository;
import com.chencraft.model.mongo.FileToken;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static com.chencraft.utils.PublisherUtils.fireAndForget;

@Service
public class FileTokenService implements Cleanable {
    private final FileTokenRepository tokenRepo;
    private final String hostname;
    private final Clock clock;

    @Autowired
    public FileTokenService(FileTokenRepository tokenRepo,
                            @Value("${app.swagger.server.url}") String hostname,
                            Clock clock) {
        this.tokenRepo = tokenRepo;
        this.hostname = hostname;
        this.clock = clock;
    }

    /**
     * If a file with the same filename is uploaded, it will be overwritten.
     */
    public String generateAccessToken(String filename) {
        FileToken token = new FileToken(filename);
        // TODO: Find all tokens for the same filename and delete them
        // TODO: Then save the new token in repository
        return this.createAccessUrl(token.getToken());
    }

    private String createAccessUrl(String uuid) {
        try {
            Method retrieveMethod = FileApi.class.getMethod("share", String.class);
            String path = retrieveMethod.getAnnotation(RequestMapping.class)
                                        .value()[0];
            String query = retrieveMethod.getParameters()[0].getAnnotation(Parameter.class).name();

            return String.format("%s%s?%s=%s", hostname, path, query, uuid);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cleanUp() {
        // Mark certificates 2 months past the used date as deleted and delete them after 1 year
        Instant now = clock.instant();
        Instant softDeleteThreshold = now.minus(Duration.ofDays(60));
        Instant hardDeleteThreshold = now.minus(Duration.ofDays(365));

        // Soft-delete: mark expired and not yet deleted
        Flux<@NonNull FileToken> softDeleteFlow = tokenRepo.findAll()
                                                           .filter(rec -> rec.getUsedAt() != null
                                                                   && rec.getUsedAt()
                                                                         .isBefore(softDeleteThreshold)
                                                                   && !rec.isDeleted())
                                                           .flatMap(rec -> {
                                                               rec.setDeleted(true);
                                                               return tokenRepo.save(rec);
                                                           });

        // Hard-delete: purge records long past the used date and already marked as deleted
        Mono<@NonNull Void> hardDeleteFlow = tokenRepo.findAll()
                                                      .filter(rec -> rec.isDeleted()
                                                              && rec.getUsedAt() != null
                                                              && rec.getUsedAt()
                                                                    .isBefore(hardDeleteThreshold))
                                                      .flatMap(rec -> tokenRepo.deleteById(rec.getId()))
                                                      .then();

        // Execute asynchronously (fire-and-forget); order soft-delete then hard-delete
        fireAndForget(softDeleteFlow.then(hardDeleteFlow));
    }
}
