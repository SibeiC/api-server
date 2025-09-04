package com.chencraft.common.service.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
public class MockMailService implements MailService {

    @Override
    public void sendMail(String to, String subject, String body) {
        log.info("MockMailService: to={}, subject={}, body={}", to, subject, body);
    }
}