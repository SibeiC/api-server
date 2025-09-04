package com.chencraft.model;

import com.chencraft.configuration.NotUndefined;
import com.chencraft.model.mongo.CertificateRecord;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * CertificatePEM
 */
@Setter
@ToString
@Validated
@NotUndefined
@EqualsAndHashCode
@NoArgsConstructor
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-22T12:16:37.352130473Z[Etc/UTC]")
public class CertificatePEM {
    @JsonProperty("certificate")
    private String certificate;

    @JsonProperty("privateKey")
    private String privateKey;

    @JsonProperty("validUntil")
    private Instant validUntil;

    @JsonIgnore
    private CertificateRecord record;

    public CertificatePEM(String certificate, String privateKey, Instant validUntil, CertificateRecord record) {
        this.certificate = certificate;
        this.privateKey = privateKey;
        this.validUntil = validUntil;
        this.record = record;
    }

    /**
     * PEM-styled certificate
     *
     * @return certificate
     **/
    @Schema(example = "-----BEGIN CERTIFICATE----- MIIDXTCCAkWgAwIBAgIJAL5k5y8z4j2mMA0GCSqGSIb3DQEBCwUAMEUxCzAJBgNV ... -----END CERTIFICATE-----", requiredMode = Schema.RequiredMode.REQUIRED, description = "PEM-styled certificate")
    @NotNull
    public String getCertificate() {
        return certificate;
    }

    /**
     * PEM-styled private key
     *
     * @return privateKey
     */
    @Schema(example = "-----BEGIN PRIVATE KEY----- MIIEvQIBADANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQ... -----END PRIVATE KEY-----", requiredMode = Schema.RequiredMode.REQUIRED, description = "PEM-styled private key")
    @NotNull
    public String getPrivateKey() {
        return privateKey;
    }

    /**
     * Timestamp when the certificate expires
     *
     * @return validUntil
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Timestamp when the certificate expires")
    @NotNull
    public Instant getValidUntil() {
        return validUntil;
    }

    @JsonIgnore
    public CertificateRecord getRecord() {
        return record;
    }
}
