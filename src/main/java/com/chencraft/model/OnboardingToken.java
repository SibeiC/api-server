package com.chencraft.model;

import com.chencraft.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;

/**
 * OnboardingToken
 */
@Setter
@ToString
@Validated
@NotUndefined
@EqualsAndHashCode
@NoArgsConstructor
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-22T12:16:37.352130473Z[Etc/UTC]")
public class OnboardingToken {
    @JsonProperty("token")
    private String key;

    @JsonProperty("validUntil")
    private Instant validUntil;

    public OnboardingToken(String key, Instant validUntil) {
        this.key = key;
        this.validUntil = validUntil;
    }

    /**
     * Onboarding token issued by the server
     *
     * @return token
     **/
    @Schema(example = "123456abcdef987654", requiredMode = Schema.RequiredMode.REQUIRED, description = "Onboarding token issued by the server")
    public @NotNull String getKey() {
        return key;
    }

    /**
     * Timestamp when the token expires
     *
     * @return validUntil
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Timestamp when the token expires")
    public @NotNull Instant getValidUntil() {
        return validUntil;
    }
}
