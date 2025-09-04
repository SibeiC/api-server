package com.chencraft.common.service.cert;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface CertificateService {
    Mono<ResponseEntity<?>> issueCertificate(String deviceId, boolean pemFormat);
}
