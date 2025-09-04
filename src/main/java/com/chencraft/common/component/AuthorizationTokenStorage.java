package com.chencraft.common.component;

import com.chencraft.model.OnboardingToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthorizationTokenStorage implements Cleanable {
    private static final int TOKEN_EXPIRATION_TIME_IN_MINUTES = 5;

    private final Map<String, OnboardingToken> tokens;
    private final Clock clock;
    private final SecureRandom random;

    @Autowired
    public AuthorizationTokenStorage(Clock clock) {
        this.tokens = new ConcurrentHashMap<>();
        this.clock = clock;
        random = new SecureRandom();
    }

    public OnboardingToken createToken() {
        Instant validUntil = clock.instant()
                                  .plusSeconds(TOKEN_EXPIRATION_TIME_IN_MINUTES * 60);
        OnboardingToken token = new OnboardingToken(getKey(), validUntil);
        tokens.put(token.getKey(), token);
        return token;
    }

    public boolean validateToken(String key) {
        return tokens.containsKey(key) && tokens.get(key).getValidUntil().isAfter(clock.instant()) && tokens.remove(key) != null;
    }

    private synchronized String getKey() {
        byte[] bytes = new byte[32]; // 256-bit token
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public void cleanUp() {
        // Remove expired tokens
        tokens.entrySet().removeIf(entry -> entry.getValue().getValidUntil().isBefore(clock.instant()));
    }
}
