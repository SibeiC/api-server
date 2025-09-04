package com.chencraft.api.secure;

import com.chencraft.common.component.AuthorizationTokenStorage;
import com.chencraft.common.service.cert.CertificateService;
import com.chencraft.model.CertificateRenewal;
import com.chencraft.model.OnboardingToken;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-22T12:16:37.352130473Z[Etc/UTC]")
@RestController
@SecurityScheme(type = SecuritySchemeType.MUTUALTLS, name = "mTLS")
public class SecureCertificateApiController implements SecureCertificateApi {

    private final AuthorizationTokenStorage tokenStorage;
    private final CertificateService certificateService;

    @Autowired
    public SecureCertificateApiController(AuthorizationTokenStorage tokenStorage,
                                          CertificateService certificateService) {
        this.tokenStorage = tokenStorage;
        this.certificateService = certificateService;
    }

    @Override
    public ResponseEntity<OnboardingToken> authorize() {
        return new ResponseEntity<>(tokenStorage.createToken(), HttpStatus.OK);
    }

    @Override
    public Mono<ResponseEntity<?>> renew(CertificateRenewal renewal) {
        // TODO: Think I can get the device ID from the mTLS certificate directly, deviceId in request should be an override instead
        return certificateService.issueCertificate(renewal.getDeviceId(), renewal.isPemFormat());
    }
}
