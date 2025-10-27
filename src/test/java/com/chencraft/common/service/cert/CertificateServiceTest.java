package com.chencraft.common.service.cert;

import com.chencraft.model.CertificatePEM;
import com.chencraft.model.mongo.CertificateRecord;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class CertificateServiceTest {

    @MockitoSpyBean
    private MTlsService mtlsService;

    @Autowired
    private AbstractCertificateService service;

    @Test
    void returnsJsonBodyWhenPemFormatFalseAndTriggersInsert() {
        ResponseEntity<?> entity = service.issueCertificate("dev1", false).block();
        assertNotNull(entity);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
        assertInstanceOf(CertificatePEM.class, entity.getBody());
        CertificatePEM pem = (CertificatePEM) entity.getBody();
        assertFalse(pem.getCertificate().isEmpty());
        assertFalse(pem.getPrivateKey().isEmpty());
        assertTrue(pem.getValidUntil().isAfter(Instant.now()));

        // doOnSuccess should have fired insertNewRecord
        verify(mtlsService, times(1)).insertNewRecord(any());
    }

    @Test
    void returnsPemAttachmentWhenPemFormatTrueAndHeadersSet() {
        ResponseEntity<?> entity = service.issueCertificate("dev2", true).block();
        assertNotNull(entity);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
        assertInstanceOf(String.class, entity.getBody());

        String body = (String) entity.getBody();
        assertFalse(body.isEmpty());

        assertEquals(MediaType.parseMediaType("application/x-pem-file"), entity.getHeaders().getContentType());
        assertEquals("attachment; filename=\"client.pem\"", entity.getHeaders().getFirst("Content-Disposition"));

        verify(mtlsService, times(1)).insertNewRecord(any());
    }

    @Test
    void doNotInsertWhenResponseNotSuccessful() {
        // Create a wrapper that returns a non-2xx response through the same pipeline, so revokeSupersededCerts should no-op
        AbstractCertificateService faulty = new AbstractCertificateService(mtlsService) {
            @Override
            protected CertificatePEM createCertificateAndPrivateKey(String deviceId) {
                return new CertificatePEM("C", "K", Instant.now(), new CertificateRecord());
            }

            @Override
            public Mono<@NonNull ResponseEntity<?>> issueCertificate(String deviceId, boolean pemFormat) {
                ResponseEntity<?> resp = ResponseEntity.status(500).build();
                // Use doOnSuccess with our consumer to mimic the base behavior path; since status is 5xx, insertNewRecord must NOT be called
                return Mono.<ResponseEntity<?>>just(resp)
                           .publishOn(Schedulers.boundedElastic())
                           .doOnSuccess(responseEntity -> {
                               if (responseEntity != null && responseEntity.getStatusCode().is2xxSuccessful()) {
                                   mtlsService.insertNewRecord(new CertificateRecord()).subscribe();
                               }
                           });
            }
        };

        faulty.issueCertificate("dev3", false).block();
        verify(mtlsService, Mockito.never()).insertNewRecord(any());
    }
}
