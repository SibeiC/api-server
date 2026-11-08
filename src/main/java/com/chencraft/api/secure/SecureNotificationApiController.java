package com.chencraft.api.secure;

import com.chencraft.common.component.AlertMessenger;
import com.chencraft.common.service.mail.MailFlag;
import com.chencraft.model.NotificationRequest;
import com.chencraft.utils.CertificateUtils;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Secure controller letting any mTLS-authenticated service send a notification email via
 * {@link AlertMessenger} (iCloud SMTP). The recipient is fixed server-side so this cannot be
 * used as an open relay; the caller's identity is taken from the verified client cert CN.
 */
@Slf4j
@RestController
@SecurityScheme(type = SecuritySchemeType.MUTUALTLS, name = "mTLS")
public class SecureNotificationApiController implements SecureNotificationApi {
    static final String UNKNOWN_SENDER = "unknown-device";

    private final AlertMessenger alertMessenger;
    private final HttpServletRequest request;

    @Autowired
    public SecureNotificationApiController(AlertMessenger alertMessenger, HttpServletRequest request) {
        this.alertMessenger = alertMessenger;
        this.request = request;
    }

    @Override
    public ResponseEntity<@NonNull String> sendNotification(NotificationRequest notification) {
        MailFlag level = notification.getLevel() == null ? MailFlag.INFO : notification.getLevel();
        alertMessenger.relayNotification(resolveSender(), level, notification.getSubject(), notification.getBody());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("queued");
    }

    /**
     * Derives the sending device from the nginx-forwarded client cert. The cert header is absent
     * in tests / non-proxied runs (the filter tolerates that when the Mongo check is not mandatory),
     * so fall back to a placeholder rather than rejecting.
     */
    private String resolveSender() {
        String clientCert = request.getHeader("X-Client-Cert");
        if (clientCert == null || clientCert.isBlank()) {
            return UNKNOWN_SENDER;
        }
        try {
            return CertificateUtils.extractCNSubject(clientCert);
        } catch (Exception e) {
            log.warn("Could not extract CN from client cert: {}", e.getMessage());
            return UNKNOWN_SENDER;
        }
    }
}
