package com.cafe.management.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public liveness endpoints. Render (and a browser opening the API hostname)
 * hits {@code GET /} by default. That path used to require a JWT, which made a
 * healthy service look like a 401 error.
 *
 * <p>These methods do not touch MySQL so a health check stays fast even when
 * the Aiven connection is cold.</p>
 */
@RestController
public class HealthController {

    @GetMapping({"/", "/health"})
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "cafe-management",
                "message", "API is running. The Angular app talks to /api/..."
        ));
    }
}
