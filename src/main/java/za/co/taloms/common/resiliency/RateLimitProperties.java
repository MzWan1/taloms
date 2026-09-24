package za.co.taloms.common.resiliency;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Centralised configuration for every API protection limit.
 *
 * Single source of truth so limits are never hard-coded at call sites. Every
 * value is environment-overridable via the {@code taloms.resiliency.*}
 * namespace, e.g. {@code TALOMS_RESILIENCY_RATELIMIT_AUTH_ENABLED=false} or
 * {@code TALOMS_RESILIENCY_UPLOAD_MAX-BYTES=52428800}.
 *
 * Tier defaults were chosen from observed TALOMS usage patterns (interactive
 * back-office users, not bulk clients) — generous enough that no normal
 * workflow is blocked, tight enough to stop runaway loops and abuse:
 *
 *   anonymous : 30 req / min per IP  → login, register, password reset only
 *   auth      : 120 req / min per IP → normal UI traffic bursts far below this
 *   admin     : 240 req / min per IP → admin screens poll counts + lists
 *   upload    : 10 req / min per IP  → large bodies, the scarcest resource
 *   company   : managed by ApiRateLimitService (per API key, not per IP)
 *
 * NOTE: rate limiting is disabled in the "test" profile (see
 * application-test.properties) so integration tests are deterministic.
 */
@Data
@ConfigurationProperties(prefix = "taloms.resiliency")
public class RateLimitProperties {

    /** Master switch for the general HTTP limiter. */
    private boolean enabled = true;

    /** The limiter tracks per client IP; this bounds its memory footprint. */
    private int maxTrackedClients = 10_000;

    private Tier anonymous = new Tier(true, 30);
    private Tier authenticated = new Tier(true, 120);
    private Tier admin = new Tier(true, 240);
    private Tier upload = new Tier(true, 10);

    /** Upload endpoints: smaller request budgets and a hard body-size cap. */
    private final Upload uploadLimits = new Upload();

    @Data
    public static class Tier {
        private boolean enabled;
        private int requestsPerMinute;

        public Tier() {}

        public Tier(boolean enabled, int requestsPerMinute) {
            this.enabled = enabled;
            this.requestsPerMinute = requestsPerMinute;
        }
    }

    @Data
    public static class Upload {
        /** Hard cap on the request body of upload endpoints, in bytes (default 20 MB). */
        private long maxBytes = 20L * 1024 * 1024;

        private Tier rate = new Tier(true, 10);
    }
}

