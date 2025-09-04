package com.chencraft.common.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {HashService.class})
public class HashServiceTest {
    @Autowired
    private HashService hashService;

    @Test
    public void githubValidationExample() {
        // https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries
        String secret = "It's a Secret to Everybody";
        String payload = "Hello, World!";
        String signature = "sha256=757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17";

        Assertions.assertTrue(hashService.validGitHubSignature(signature, payload, secret));
    }
}