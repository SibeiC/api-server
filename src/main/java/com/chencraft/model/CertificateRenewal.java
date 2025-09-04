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

@Setter
@ToString
@Validated
@NotUndefined
@EqualsAndHashCode
public class CertificateRenewal {
    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("pemFormat")
    private boolean pemFormat = false;

    /**
     * Get deviceId
     *
     * @return deviceId
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Device name, used for certificate CN", defaultValue = "hostname")
    @Valid
    @NotNull
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Get pemFormat
     *
     * @return pemFormat
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Whether the certificate is in PEM format", defaultValue = "false")
    @NotNull
    public boolean isPemFormat() {
        return pemFormat;
    }
}
