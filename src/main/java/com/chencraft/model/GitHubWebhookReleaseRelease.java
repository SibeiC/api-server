package com.chencraft.model;

import com.chencraft.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHubWebhookReleaseRelease
 */
@Setter
@ToString
@Validated
@NotUndefined
@EqualsAndHashCode
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-09T11:04:45.539601364Z[Etc/UTC]")
public class GitHubWebhookReleaseRelease {
    @JsonProperty("assets")
    @Valid
    private List<GitHubWebhookReleaseReleaseAssets> assets = new ArrayList<>();

    /**
     * Get assets
     *
     * @return assets
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    public List<GitHubWebhookReleaseReleaseAssets> getAssets() {
        return assets;
    }
}
