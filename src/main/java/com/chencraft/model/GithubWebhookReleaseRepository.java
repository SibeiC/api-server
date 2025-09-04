package com.chencraft.model;

import com.chencraft.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

@Setter
@ToString
@Validated
@NotUndefined
@EqualsAndHashCode
public class GithubWebhookReleaseRepository {
    @JsonProperty("full_name")
    private String fullName = null;

    /**
     * Repository's full name
     *
     * @return full_name
     **/
    @Schema(example = "SibeiC/api-server", requiredMode = Schema.RequiredMode.REQUIRED, description = "Repository full name")
    @NotNull
    public String getFullName() {
        return fullName;
    }
}
