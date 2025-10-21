package com.chencraft.common.filter;


import com.chencraft.common.component.AlertMessenger;
import com.chencraft.common.service.cert.MTlsService;
import com.chencraft.model.mongo.CertificateRecord;
import com.chencraft.utils.CertificateUtils;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;

@Slf4j
@Component
public class MtlsVerificationFilter extends OncePerRequestFilter {

    private final MTlsService mtlsService;
    private final AlertMessenger alertMessenger;
    private final Clock clock;
    private final boolean mongoCheckMandatory;

    @Autowired
    public MtlsVerificationFilter(MTlsService mtlsService,
                                  AlertMessenger alertMessenger,
                                  Clock clock,
                                  @Value("${app.mtls.mongo-check-mandatory}") boolean mongoCheckMandatory) {
        this.mtlsService = mtlsService;
        this.alertMessenger = alertMessenger;
        this.clock = clock;
        this.mongoCheckMandatory = mongoCheckMandatory;
    }

    @Override
    protected boolean shouldNotFilter(@Nonnull HttpServletRequest request) {
        // Only apply filter to /secure/** endpoints
        return !request.getRequestURI().startsWith("/secure/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain) throws ServletException, IOException {
        String verify = request.getHeader("X-Client-Verify");

        if (!"SUCCESS".equalsIgnoreCase(verify)) {
            log.warn("mTLS verification failed for {}: {}", request.getRequestURI(), verify);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "mTLS required");
            return;
        }

        String clientCert = request.getHeader("X-Client-Cert");
        if (clientCert != null) {
            // Client cert should always be present when proxied through nginx, it is not available when running tests
            String fingerprint = CertificateUtils.computeSha256Fingerprint(clientCert);
            log.debug("Client certificate fingerprint: {}", fingerprint);

            CertificateRecord certRecord = mtlsService.findByFingerprint(fingerprint)
                                                      .blockOptional()
                                                      .orElse(null);

            if (certRecord == null) {
                if (mongoCheckMandatory) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Certificate record not found");
                    return;
                } else {
                    // TODO: Once all production records are in mongo, remove this bypass, aiming at 3 months later, today: 2025-09-07
                    log.warn("Certificate not found for {}: {}", request.getRequestURI(), fingerprint);
                }
            }

            // Certificate revocation check
            if (certRecord != null && certRecord.getRevokedAt() != null) {
                log.warn("Certificate revoked for {} on {}", fingerprint, certRecord.getRevokedAt().atZone(clock.getZone()));
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Certificate revoked");
                alertMessenger.alertRevokedCertificateAccess(certRecord, request.getRequestURI());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
