package com.chencraft.utils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class PublisherUtils {
    public static <T> void fireAndForget(Mono<@NonNull T> mono) {
        mono.subscribeOn(Schedulers.boundedElastic())
            .doOnError(e -> log.error("Failure in mono", e))
            .subscribe();
    }

    public static <T> void fireAndForget(Flux<@NonNull T> flux) {
        flux.subscribeOn(Schedulers.boundedElastic())
            .doOnError(e -> log.error("Failure in flux", e))
            .subscribe();
    }
}
