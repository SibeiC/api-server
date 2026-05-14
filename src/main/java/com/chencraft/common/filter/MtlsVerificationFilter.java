package com.chencraft.common.filter;


import com.chencraft.common.component.AlertMessenger;
import com.chencraft.common.service.cert.MTlsService;
import com.chencraft.model.mongo.CertificateRecord;
import com.chencraft.utils.CertificateUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;

@Slf4j
@Component
public class MtlsVerificationFilter extends OncePerRequestFilter {

    private final MTlsService mtlsService;
    private final AlertMessenger alertMessenger;
    private final Clock clock;
    private final boolean mongoCheckMandatory;
    private final byte[] expectedProxySecret;

    @Autowired
    public MtlsVerificationFilter(MTlsService mtlsService,
                                  AlertMessenger alertMessenger,
                                  Clock clock,
                                  @Value("${app.mtls.mongo-check-mandatory}") boolean mongoCheckMandatory,
                                  @Value("${app.mtls.proxy-secret:}") String proxySecret) {
        this.mtlsService = mtlsService;
        this.alertMessenger = alertMessenger;
        this.clock = clock;
        this.mongoCheckMandatory = mongoCheckMandatory;
        this.expectedProxySecret = (proxySecret == null || proxySecret.isBlank())
                ? null
                : proxySecret.getBytes(StandardCharsets.UTF_8);
    }

    @PostConstruct
    void logProxySecretMode() {
        if (expectedProxySecret == null) {
            log.warn("app.mtls.proxy-secret is unset — X-Proxy-Secret check is DISABLED. "
                    + "Set APP_MTLS_PROXY_SECRET and inject it from nginx to enforce.");
        } else {
            log.info("X-Proxy-Secret check ENABLED ({} bytes expected)", expectedProxySecret.length);
        }
    }

    @Override
    protected boolean shouldNotFilter(@Nonnull HttpServletRequest request) {
        // Only apply filter to /secure/** endpoints
        return !request.getRequestURI().startsWith("/secure/");
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain) throws ServletException, IOException {
        // Defense in depth: when configured, the request must carry the shared secret nginx injects.
        // This stops any peer that reaches the upstream directly from forging X-Client-Verify/X-Client-Cert.
        if (expectedProxySecret != null) {
            String provided = request.getHeader("X-Proxy-Secret");
            byte[] providedBytes = provided == null ? new byte[0] : provided.getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(expectedProxySecret, providedBytes)) {
                log.warn("Rejecting {} — missing or invalid X-Proxy-Secret", request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Proxy authentication required");
                return;
            }
        }

        String verify = request.getHeader("X-Client-Verify");

        if (!"SUCCESS".equalsIgnoreCase(verify)) {
            log.warn("mTLS verification failed for {}: {}", request.getRequestURI(), verify);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "mTLS required");
            return;
        }

        String clientCert = request.getHeader("X-Client-Cert");

        if (mongoCheckMandatory && clientCert == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Client certificate missing from nginx proxy");
            return;
        }

        if (clientCert != null) {
            // Client cert should always be present when proxied through nginx, it is not available when running tests
            String fingerprint;
            try {
                fingerprint = CertificateUtils.computeSha256Fingerprint(clientCert);
            } catch (RuntimeException e) {
                log.warn("Rejecting {} — X-Client-Cert is not a valid PEM: {}",
                        request.getRequestURI(), e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid client certificate");
                return;
            }
            log.debug("Client certificate fingerprint: {}", fingerprint);

            CertificateRecord certRecord = mtlsService.findByFingerprint(fingerprint)
                                                      .blockOptional()
                                                      .orElse(null);

            if (certRecord == null && mongoCheckMandatory) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Certificate record not found");
                return;
            }

            // Certificate revocation check
            if (certRecord != null && certRecord.getRevokedAt() != null) {
                log.warn("Certificate revoked for {} on {}", fingerprint, certRecord.getRevokedAt()
                                                                                    .atZone(clock.getZone()));
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Certificate revoked");
                alertMessenger.alertRevokedCertificateAccess(certRecord, request.getRequestURI());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
