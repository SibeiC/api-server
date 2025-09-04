package com.chencraft.model;

import com.chencraft.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

/**
 * GitHubWebhookRelease
 */
@Setter
@ToString
@Validated
@NotUndefined
@EqualsAndHashCode
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-09T11:04:45.539601364Z[Etc/UTC]")
public class GitHubWebhookRelease {
    /**
     * Action type of the webhook
     */
    public enum ActionEnum {
        CREATED("created"),
        DELETED("deleted"),
        EDITED("edited"),
        PRERELEASED("prereleased"),
        PUBLISHED("published"),
        RELEASED("released"),
        UNPUBLISHED("unpublished");

        private final String value;

        ActionEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static ActionEnum fromValue(String text) {
            for (ActionEnum b : ActionEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    @JsonProperty("action")
    private ActionEnum action = null;

    @JsonProperty("release")
    private GitHubWebhookReleaseRelease release = null;

    @JsonProperty("repository")
    private GithubWebhookReleaseRepository repository = null;


    /**
     * Action type of the webhook
     *
     * @return action
     **/
    @Schema(example = "released", requiredMode = Schema.RequiredMode.REQUIRED, description = "Action type of the webhook")
    @NotNull
    public ActionEnum getAction() {
        return action;
    }

    /**
     * Get release
     *
     * @return release
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    public GitHubWebhookReleaseRelease getRelease() {
        return release;
    }

    /**
     * Get repository
     *
     * @return repository
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Valid
    public GithubWebhookReleaseRepository getRepository() {
        return repository;
    }
}
