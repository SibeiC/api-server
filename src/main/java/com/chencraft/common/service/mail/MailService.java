package com.chencraft.common.service.mail;

public interface MailService {
    /**
     * Sends an email with the specified flag.
     *
     * @param to      recipient email address
     * @param flag    severity flag
     * @param subject email subject
     * @param body    email body
     */
    void sendMail(String to, MailFlag flag, String subject, String body);

}
