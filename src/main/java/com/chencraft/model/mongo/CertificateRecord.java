package com.chencraft.model.mongo;

import com.chencraft.utils.CertificateUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.security.cert.X509Certificate;
import java.time.Instant;

@Document("certificates")
@CompoundIndex(name = "fingerprint_deleted_idx", def = "{'fingerprintSha256': 1, 'isDeleted': 1}", unique = true)
@Data
@NoArgsConstructor
public class CertificateRecord {
    @Id
    private String id;

    /**
     * SHA-256 fingerprint of the certificate (hex-encoded).
     * This is your unique identifier for the cert.
     */
    @Indexed(unique = true)
    private String fingerprintSha256;

    /**
     * Identifier of the machine this cert was issued to.
     */
    private String machineId;

    /**
     * Timestamps for cert lifecycle.
     */
    private Instant issuedAt;
    private Instant expiresAt;
    private Instant revokedAt;

    /**
     * Optional reason for revocation (if revoked).
     */
    private String revokeReason;

    /**
     * Logical deletion flag
     */
    public boolean isDeleted = false;

    @Version
    private Long version; // for optimistic locking

    public CertificateRecord(X509Certificate certificate, String machineId) {
        this.fingerprintSha256 = CertificateUtils.computeSha256Fingerprint(certificate);
        this.machineId = machineId;
        this.issuedAt = certificate.getNotBefore().toInstant();
        this.expiresAt = certificate.getNotAfter().toInstant();
    }
}
