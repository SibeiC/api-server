package com.chencraft.model;

import com.chencraft.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.http.HttpStatusCode;
import org.springframework.validation.annotation.Validated;

/**
 * Error
 */
@Setter
@Validated
@NotUndefined
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-09T11:04:45.539601364Z[Etc/UTC]")
public class ErrorResponse {
    @JsonProperty("code")
    private int code;

    @JsonIgnore
    private HttpStatusCode status = null;

    @JsonProperty("message")
    private String message = null;

    public ErrorResponse status(HttpStatusCode code) {
        this.status = code;
        return this;
    }

    public ErrorResponse message(String message) {
        this.message = message;
        return this;
    }

    /**
     * Get code
     *
     * @return code
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "500")
    @NotNull
    public int getCode() {
        return status.value();
    }

    /**
     * Get message
     *
     * @return message
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "Brief description of the error")
    @NotNull
    public String getMessage() {
        return message;
    }
}
