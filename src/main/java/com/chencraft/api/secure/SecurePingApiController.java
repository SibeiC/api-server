package com.chencraft.api.secure;

import lombok.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurePingApiController implements SecurePingApi {
    @Override
    public ResponseEntity<@NonNull String> ping() {
        return ResponseEntity.ok("pong");
    }
}
