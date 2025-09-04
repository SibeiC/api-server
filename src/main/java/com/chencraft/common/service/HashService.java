package com.chencraft.common.service;

import com.chencraft.api.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
public class HashService {
    public boolean validGitHubSignature(String signature, String rawBody, String secret) {
        String hashingAlgorithm = "HmacSHA256";
        String prefix = "sha256=";

        if (signature == null || rawBody == null || secret == null) {
            return false;
        }
        String hash = this.encodeWithSHA256(hashingAlgorithm, prefix, secret, rawBody);
        log.debug("Signature: {}, Raw body: {}, Hash: {}", signature, rawBody, hash);

        return signature.equals(hash);
    }

    private String encodeWithSHA256(String hashingAlgorithm, String prefix, String secret, String payload) {
        try {
            Mac algo = Mac.getInstance(hashingAlgorithm);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), hashingAlgorithm);
            algo.init(secretKey);
            byte[] hash = algo.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            return prefix + new String(Hex.encode(hash));
        } catch (NoSuchAlgorithmException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find algorithm for hashing with " + hashingAlgorithm, e);
        } catch (InvalidKeyException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Given key: " + secret + " is inappropriate for initializing this MAC", e);
        }
    }
}
