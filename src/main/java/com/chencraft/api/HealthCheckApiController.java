package com.chencraft.api;

import lombok.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckApiController implements HealthCheckApi {
    @Override
    public ResponseEntity<@NonNull String> healthCheck() {
        return ResponseEntity.ok("ok");
    }
}
