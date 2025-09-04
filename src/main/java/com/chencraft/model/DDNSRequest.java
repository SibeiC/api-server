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
public class DDNSRequest {
    @JsonProperty
    private String hostname;

    @JsonProperty
    private String dnsType;

    @JsonProperty
    private String myIp = null;

    @JsonProperty
    private Boolean proxied = null;

    @Valid
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Hostname to be updated", example = "www.chencraft.com")
    public String getHostname() {
        return hostname;
    }

    @Valid
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "DNS record type", example = "A", defaultValue = "A")
    public String getDnsType() {
        return dnsType;
    }

    @Valid
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Server IP", example = "1.1.1.1")
    public String getMyIp() {
        return myIp;
    }

    @Valid
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Should be proxied by Cloudflare", example = "false", defaultValue = "null")
    public Boolean isProxied() {
        return proxied;
    }
}
