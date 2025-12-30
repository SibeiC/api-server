package com.chencraft.common;

import com.chencraft.common.component.AlertMessenger;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * This class is used for testing purposes only.
 */
@Slf4j
@Component
public class TestEntryPoint implements CommandLineRunner {
    @Value("${app.mail.test-message:false}")
    private boolean triggerTestEmail;

    private final AlertMessenger alertMessenger;

    @Autowired
    public TestEntryPoint(AlertMessenger alertMessenger) {
        this.alertMessenger = alertMessenger;
    }

    @Override
    public void run(@Nullable String... args) throws Exception {
        if (triggerTestEmail) {
            this.alertMessenger.testAlert();
        }
    }
}
