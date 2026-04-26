package com.chencraft.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HealthCheckTargetRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String url;

    @Min(100)
    private Integer expectedStatus;

    @Min(1)
    private Long timeoutSeconds;

    @Min(1)
    private Long failureThresholdMinutes;

    @Min(0)
    private Integer retryAttempts;

    @Min(0)
    private Long retryDelaySeconds;
}
