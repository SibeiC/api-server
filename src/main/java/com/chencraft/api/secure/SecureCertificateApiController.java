package com.chencraft.api.secure;

import com.chencraft.common.component.AuthorizationTokenStorage;
import com.chencraft.common.service.cert.CertificateService;
import com.chencraft.common.service.cert.MTlsService;
import com.chencraft.model.CertificateRenewal;
import com.chencraft.model.CertificateRevokeRequest;
import com.chencraft.model.OnboardingToken;
import com.chencraft.utils.CertificateUtils;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Secure certificate endpoints protected by mTLS. Issues onboarding tokens and processes renewals.
 * Delegates to CertificateService for PKI operations.
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-22T12:16:37.352130473Z[Etc/UTC]")
@RestController
@SecurityScheme(type = SecuritySchemeType.MUTUALTLS, name = "mTLS")
public class SecureCertificateApiController implements SecureCertificateApi {

    private final AuthorizationTokenStorage tokenStorage;
    private final CertificateService certificateService;
    private final MTlsService mTlsService;
    private final HttpServletRequest request;


    /**
     * Constructs SecureCertificateApiController.
     *
     * @param tokenStorage       storage for onboarding tokens
     * @param certificateService certificate issuance/renewal service
     */
    @Autowired
    public SecureCertificateApiController(AuthorizationTokenStorage tokenStorage,
                                          CertificateService certificateService,
                                          MTlsService mTlsService,
                                          HttpServletRequest request) {
        this.tokenStorage = tokenStorage;
        this.certificateService = certificateService;
        this.mTlsService = mTlsService;
        this.request = request;
    }

    /**
     * Issues a one-time onboarding token used by devices to authorize certificate issuance.
     *
     * @return HTTP 200 with a generated token
     */
    @Override
    public ResponseEntity<@NonNull OnboardingToken> authorize() {
        return new ResponseEntity<>(tokenStorage.createToken(), HttpStatus.OK);
    }

    /**
     * Renews a device certificate using provided device ID. Returns a PEM bundle when requested.
     *
     * @param renewal payload containing deviceId and pemFormat flag
     * @return reactive ResponseEntity from CertificateService
     */
    @Override
    public Mono<ResponseEntity<?>> renew(CertificateRenewal renewal) {
        if (renewal.getDeviceId() == null) {
            String clientCert = request.getHeader("X-Client-Cert");
            String requester = CertificateUtils.extractCNSubject(clientCert);
            renewal.setDeviceId(requester);
        }
        return certificateService.issueCertificate(renewal.getDeviceId(), renewal.isPemFormat());
    }

    @Override
    public Mono<ResponseEntity<@NonNull String>> revoke(CertificateRevokeRequest revokeRequest) {
        if ((revokeRequest.getMongoId() == null || revokeRequest.getMongoId().isBlank()) &&
                (revokeRequest.getFingerprintSha256() == null || revokeRequest.getFingerprintSha256().isBlank()) &&
                (revokeRequest.getDeviceId() == null || revokeRequest.getDeviceId().isBlank())) {
            // Keep the error format for bad request to help clients debug
            return Mono.just(ResponseEntity.badRequest().body("One of mongoId, deviceId, or fingerprintSha256 is required"));
        }

        String reason = revokeRequest.getRevokeReason();

        if (revokeRequest.getMongoId() != null && !revokeRequest.getMongoId().isBlank()) {
            return mTlsService.revokeById(revokeRequest.getMongoId(), reason)
                              .map(found -> ResponseEntity.ok(found ? "1 record affected. " : "0 records affected."));
        }

        if (revokeRequest.getFingerprintSha256() != null && !revokeRequest.getFingerprintSha256().isBlank()) {
            return mTlsService.revokeByFingerprint(revokeRequest.getFingerprintSha256(), reason)
                              .map(found -> ResponseEntity.ok(found ? "1 record affected. " : "0 records affected."));
        }

        // deviceId path: revoke all active certificates for the device
        return mTlsService.revokeByDeviceId(revokeRequest.getDeviceId(), reason)
                          .map(count -> ResponseEntity.ok(count + " records affected. "));
    }
}
