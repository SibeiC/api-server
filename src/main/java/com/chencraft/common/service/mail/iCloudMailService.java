package com.chencraft.common.service.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * MailService implementation backed by Spring's JavaMailSender configured for iCloud SMTP.
 * External IO: sends email via SMTP using credentials provided in configuration (app.mail.*).
 */
@Lazy
@Service
public class iCloudMailService implements MailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.sender}")
    private String sender;

    /**
     * Creates the iCloudMailService.
     *
     * @param mailSender JavaMailSender configured for iCloud SMTP
     */
    @Autowired
    public iCloudMailService(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendMail(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(sender);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }
}
