package com.chencraft.common.service.cert;

import lombok.NonNull;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface CertificateService {
    Mono<@NonNull ResponseEntity<?>> issueCertificate(String deviceId, boolean pemFormat);
}
