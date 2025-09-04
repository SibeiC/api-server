package com.chencraft.model;

import com.chencraft.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

/**
 * GitHubWebhookReleaseReleaseAssets
 */
@Setter
@ToString
@Validated
@NotUndefined
@EqualsAndHashCode
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-09T11:04:45.539601364Z[Etc/UTC]")
public class GitHubWebhookReleaseReleaseAssets {
    @JsonProperty("url")
    private String url = null;

    @JsonProperty("name")
    private String name = null;

    /**
     * URL for fetching asset
     *
     * @return url
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "URL for fetching asset", defaultValue = "https://example.com/file.pdf")
    @NotNull
    public String getUrl() {
        return url;
    }


    /**
     * Asset's filename
     *
     * @return name
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Asset's filename", defaultValue = "file.pdf")
    @NotNull
    public String getName() {
        return name;
    }
}
