package com.chencraft.common.component;

import com.chencraft.common.service.executor.TaskExecutor;
import com.chencraft.common.service.mail.MailService;
import com.chencraft.model.mongo.CertificateRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class AlertMessenger {
    private final TaskExecutor taskExecutor;
    private final MailService mailService;

    @Value("${app.alert.email}")
    private String defaultRecipient;

    @Autowired
    public AlertMessenger(TaskExecutor taskExecutor, MailService mailService) {
        this.taskExecutor = taskExecutor;
        this.mailService = mailService;
    }

    public void alertCertificateExpiring(String hostname, LocalDate validUntil) {
        log.info("Sending alert for certificate expiring soon: {}", hostname);

        String subject = "Certificate for " + hostname + " is expiring soon";
        String body = "Certificate for " + hostname + " will expire on " + validUntil + ".\n\n"
                + "Certificate issuance script is available at https://github.com/SibeiC/ToyUtils/blob/master/mTLS/intermediate.sh\n\n"
                + "Installation script is available at https://github.com/SibeiC/ToyUtils/blob/master/mTLS/install_intermediate.sh";

        taskExecutor.execute(() -> mailService.sendMail(defaultRecipient, subject, body));
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
