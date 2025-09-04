package com.chencraft.common.service.cert;

import com.chencraft.common.mongo.CertificateRepository;
import com.chencraft.model.CertificatePEM;
import com.chencraft.model.mongo.CertificateRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static com.chencraft.utils.PublisherUtils.fireAndForget;

@RequiredArgsConstructor
public abstract class AbstractCertificateService implements CertificateService {
    protected final CertificateRepository mongoRepository;

    @Override
    public Mono<ResponseEntity<?>> issueCertificate(String deviceId, boolean pemFormat) {
        CertificatePEM certificatePem = createCertificateAndPrivateKey(deviceId);
        ResponseEntity<?> response;

        if (pemFormat) {
            String pemContent = certificatePem.getCertificate() + certificatePem.getPrivateKey();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/x-pem-file"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"client.pem\"");
            response = new ResponseEntity<>(pemContent, headers, HttpStatus.OK);
        } else {
            response = new ResponseEntity<>(certificatePem, HttpStatus.OK);
        }

        return Mono.<ResponseEntity<?>>just(response)
                   .doOnSuccess(revokeSupersededCerts(certificatePem.getRecord()));
    }

    private Consumer<ResponseEntity<?>> revokeSupersededCerts(CertificateRecord record) {
        return responseEntity -> {
            if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful()) {
                return;
            }
            fireAndForget(mongoRepository.save(record)
                                         .flatMap(savedRecord ->
                                                          mongoRepository.findByMachineIdAndIsDeletedFalse(savedRecord.getMachineId())
                                                                         .filter(old -> !old.getFingerprintSha256().equals(savedRecord.getFingerprintSha256()))
                                                                         .flatMap(old -> {
                                                                             // Revoke old cert
                                                                             old.setRevokedAt(record.getIssuedAt());
                                                                             old.setRevokeReason("Superseded by new certificate");
                                                                             return mongoRepository.save(old);
                                                                         })
                                                                         .then(Mono.just(savedRecord))));
        };
    }

    protected abstract CertificatePEM createCertificateAndPrivateKey(String deviceId);
}
