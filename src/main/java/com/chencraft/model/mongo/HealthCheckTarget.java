package com.chencraft.model.mongo;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("healthCheckTargets")
@Data
@NoArgsConstructor
public class HealthCheckTarget {
    public enum Status {
        UNKNOWN,
        UP,
        DOWN
    }

    @Id
    private String id;

    @Indexed
    private String name;

    private String url;

    private int expectedStatus = 200;

    private long timeoutSeconds = 10;

    private long failureThresholdMinutes = 60;

    private int retryAttempts = 2;

    private long retryDelaySeconds = 2;

    private Instant lastCheckedAt;
    private Instant lastSuccessAt;
    private Instant alertedAt;

    private Status lastStatus = Status.UNKNOWN;
    private Integer lastResponseCode;
    private String lastError;

    private boolean isDeleted = false;

    @Version
    private Long version;

    public HealthCheckTarget(String name, String url) {
        this.name = name;
        this.url = url;
    }
}
