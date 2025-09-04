package com.chencraft.api;

import com.chencraft.common.component.AuthorizationTokenStorage;
import com.chencraft.common.service.cert.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-22T12:16:37.352130473Z[Etc/UTC]")
@RestController
public class CertificateApiController implements CertificateApi {
    private final AuthorizationTokenStorage authTokenStorage;
    private final CertificateService certificateService;

    @Autowired
    public CertificateApiController(AuthorizationTokenStorage authTokenStorage,
                                    CertificateService certificateService) {
        this.authTokenStorage = authTokenStorage;
        this.certificateService = certificateService;
    }

    @Override
    public Mono<ResponseEntity<?>> certificateIssue(String token, String deviceId, boolean pemFormat) {
        if (!authTokenStorage.validateToken(token)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        return certificateService.issueCertificate(deviceId, pemFormat);
    }
}
