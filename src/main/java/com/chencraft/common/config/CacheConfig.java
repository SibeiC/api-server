package com.chencraft.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean
    public CaffeineCacheManager cacheManager(MeterRegistry meterRegistry) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("certificatesByFingerprint");
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterWrite(Duration.ofHours(1))
                        .recordStats()
        );
        cacheManager.setAsyncCacheMode(true);

//        // Register cache metrics with Prometheus (bind Caffeine native cache)
//        cacheManager.getCacheNames().forEach(name -> {
//            org.springframework.cache.Cache springCache = cacheManager.getCache(name);
//            if (springCache instanceof CaffeineCache caffeineCache) {
//                com.github.benmanes.caffeine.cache.Cache<?, ?> nativeCache = caffeineCache.getNativeCache();
//                CaffeineCacheMetrics.monitor(
//                        meterRegistry,
//                        nativeCache,
//                        caffeineCache.getName()
//                );
//            }
//        });

        return cacheManager;
    }
}