package com.chencraft.api;

import com.chencraft.common.component.AuthorizationTokenStorage;
import com.chencraft.common.service.cert.CertificateService;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller implementing CertificateApi for certificate issuance endpoints.
 * Secured access for /secure variants is configured in SecurityConfig.
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-22T12:16:37.352130473Z[Etc/UTC]")
@RestController
public class CertificateApiController implements CertificateApi {
    private final AuthorizationTokenStorage authTokenStorage;
    private final CertificateService certificateService;

    /**
     * Constructs the controller with required collaborators.
     *
     * @param authTokenStorage storage for validating onboarding tokens
     * @param certificateService service used to issue or renew certificates
     */
    @Autowired
    public CertificateApiController(AuthorizationTokenStorage authTokenStorage,
                                   CertificateService certificateService) {
        this.authTokenStorage = authTokenStorage;
        this.certificateService = certificateService;
    }

    /**
     * Issues a device certificate if the provided onboarding token is valid.
     *
     * @param token     onboarding token to authorize the request
     * @param deviceId  unique device identifier used within the PKI
     * @param pemFormat when true, response will be a PEM bundle; otherwise a structured payload
     * @return Mono emitting 401 Unauthorized on invalid token or the issuance response from CertificateService
     */
    @Override
    public Mono<@NonNull ResponseEntity<?>> certificateIssue(String token, String deviceId, boolean pemFormat) {
        if (!authTokenStorage.validateToken(token)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        return certificateService.issueCertificate(deviceId, pemFormat);
    }
}
