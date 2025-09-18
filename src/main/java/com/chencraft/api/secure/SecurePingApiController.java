package com.chencraft.api.secure;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurePingApiController implements SecurePingApi {
    @Override
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
