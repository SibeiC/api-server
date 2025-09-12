package com.chencraft.model;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for revoking certificate records. At least one identifier must be provided
 * to locate the certificate(s) to revoke.
 */
@Data
@NoArgsConstructor
public class CertificateRevokeRequest {
    /**
     * Mongo document id of the certificate record.
     */
    private String mongoId;

    /**
     * The device identifier (machineId) that certificate(s) were issued to.
     */
    private String deviceId;

    /**
     * SHA-256 fingerprint (hex) of the certificate to revoke.
     */
    @Size(min = 64, max = 64)
    private String fingerprintSha256;

    /**
     * Optional revoke reason to store alongside the record.
     */
    private String revokeReason;
}
