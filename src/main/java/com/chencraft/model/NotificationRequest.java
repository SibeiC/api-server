package com.chencraft.model;

import com.chencraft.common.service.mail.MailFlag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for relaying a notification email from an mTLS-authenticated service.
 * The recipient is fixed server-side (app.alert.recipient); callers cannot choose it.
 */
@Data
@NoArgsConstructor
public class NotificationRequest {
    @NotBlank
    @Size(max = 200)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Email subject (CR/LF are collapsed to spaces)", example = "Backup finished")
    private String subject;

    @NotBlank
    @Size(max = 20000)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Plain-text body; HTML is escaped, newlines are preserved", example = "Nightly backup completed in 42s.")
    private String body;

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Severity flag shown in the subject prefix", example = "INFO", defaultValue = "INFO")
    private MailFlag level;
}
