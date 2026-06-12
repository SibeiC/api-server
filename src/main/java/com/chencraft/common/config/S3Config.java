package com.chencraft.common.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Lazy
@Getter
@Configuration
public class S3Config {
    /**
     * Configuration class for R2 credentials and endpoint
     */
    private final String accessKey;
    private final String secretKey;
    private final String endpoint;

    @Autowired
    public S3Config(@Value("${cloudflare.r2.account-id}") String accountId,
                    @Value("${cloudflare.r2.access-key}") String accessKey,
                    @Value("${cloudflare.r2.secret-key}") String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endpoint = String.format("https://%s.r2.cloudflarestorage.com", accountId);
    }
}
