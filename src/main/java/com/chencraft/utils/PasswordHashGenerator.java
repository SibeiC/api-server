package com.chencraft.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PasswordHashGenerator implements CommandLineRunner {

    @Value("${app.security.generate-hash:false}")
    private boolean generateHash;

    @Value("${app.security.plain-password:}")
    private String plainPassword;

    @Override
    public void run(String... args) {
        if (generateHash && plainPassword != null && !plainPassword.isEmpty()) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String hash = encoder.encode(plainPassword);
            log.info("Generated BCrypt hash: {}", hash);
        } else if (generateHash) {
            log.warn("No plain password provided, skipping hash generation");
        }
    }
}