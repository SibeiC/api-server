package com.chencraft.common.component;

import com.chencraft.common.service.executor.TaskExecutor;
import com.chencraft.common.service.mail.MailService;
import com.chencraft.model.mongo.CertificateRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Sends operational alert emails for notable events in the system (e.g., certificate lifecycle, security signals).
 * <p>
 * Responsibilities
 * - Provides small, purpose-specific helper methods that format and dispatch email notifications via MailService.
 * - Executes email sending on the TaskExecutor to avoid blocking caller threads.
 * <p>
 * Thread-safety
 * - Stateless Spring singleton; delegates side effects to MailService.
 * <p>
 * External I/O
 * - Sends emails using the configured MailService implementation.
 * <p>
 * Configuration
 * - Uses app.alert.recipient as the default recipient for all alerts.
 */
@Slf4j
@Component
public class AlertMessenger {
    private final TaskExecutor taskExecutor;
    private final MailService mailService;

    @Value("${app.alert.recipient}")
    private String defaultRecipient;

    @Autowired
    public AlertMessenger(TaskExecutor taskExecutor, MailService mailService) {
        this.taskExecutor = taskExecutor;
        this.mailService = mailService;
    }

    /**
     * Sends an alert email that the certificate for the given host is expiring (or has expired).
     *
     * @param hostname                  identifier of the machine or host the certificate was issued to; not null
     * @param validUntil                certificate validity end date (LocalDate in system/default zone); not null
     * @param includeInstallSuggestions whether to append issuance/installation guidance links to the message
     */
    public void alertCertificateExpiring(String hostname, LocalDate validUntil, boolean includeInstallSuggestions) {
        log.info("Sending alert for certificate expiring soon: {}", hostname);

        String subject = "Certificate for " + hostname + " is expiring soon";
        String body = "Certificate for " + hostname + " will expire on " + validUntil + ".";

        if (includeInstallSuggestions) {
            body += """
                    
                    
                    Certificate issuance script is available at https://github.com/SibeiC/ToyUtils/blob/master/mTLS/intermediate.sh
                    
                    Installation script is available at https://github.com/SibeiC/ToyUtils/blob/master/mTLS/install_intermediate.sh""";
        }

        String finalBody = body;
        taskExecutor.execute(() -> mailService.sendMail(defaultRecipient, subject, finalBody));
    }

    public void alertUnauthorizedGitHubToken(String repoName) {
        log.info("Sending alert for unauthorized GitHub token: {}", repoName);

        String subject = "Unauthorized GitHub token detected";
        String body = "Unauthorized GitHub token detected for repository " + repoName + ".\n\n"
                + "Please check your GitHub token and/or retrigger the endpoint.";
        taskExecutor.execute(() -> mailService.sendMail(defaultRecipient, subject, body));
    }

    public void alertRevokedCertificateAccess(CertificateRecord certRecord, String endpoint) {
        log.info("Sending alert for revoked certificate access: {}", certRecord.getFingerprintSha256());

        String subject = "Revoked certificate detected";
        String body = "Someone attempted to access endpoint " + endpoint + " with a revoked certificate issued for " + certRecord.getMachineId() + ".\n\n"
                + "Certificate fingerprint: " + certRecord.getFingerprintSha256();
        taskExecutor.execute(() -> mailService.sendMail(defaultRecipient, subject, body));
    }
}
