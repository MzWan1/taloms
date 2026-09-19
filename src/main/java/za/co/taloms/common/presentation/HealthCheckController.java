package za.co.taloms.common.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Liveness + readiness health endpoint.
 *
 * /health       — liveness: is the application running? No dependency checks,
 *                 so it stays green (and cheap) even during a database outage.
 * /health?deep  — readiness: can the application actually PERFORM its core
 *                 operation (a trivial database round-trip)?
 *
 * The response intentionally exposes no internals — no host names, no driver
 * versions, no credentials, no SQL. Load balancers (Render health checks) can
 * point at /health, while operators can probe /health?deep manually.
 */
@RestController
@RequiredArgsConstructor
public class HealthCheckController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(body);
    }

    @GetMapping(path = "/health", params = "deep")
    public ResponseEntity<Map<String, Object>> deepHealth() {
        Map<String, Object> body = new LinkedHashMap<>();
        boolean databaseUp;
        try {
            // The cheapest possible real round-trip through the connection pool.
            databaseUp = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                    "SELECT TRUE", Boolean.class));
        } catch (Exception e) {
            databaseUp = false;
        }

        body.put("status", databaseUp ? "UP" : "DEGRADED");
        body.put("checks", Map.of("database", databaseUp ? "UP" : "DOWN"));
        body.put("timestamp", Instant.now().toString());

        return ResponseEntity.status(databaseUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(body);
    }
}
