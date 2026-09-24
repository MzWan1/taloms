package za.co.taloms.common.resiliency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * General HTTP rate limiter for TALOMS' own (browser/JWT) endpoints.
 *
 * Uses an in-memory fixed-window algorithm. State is per JVM instance; horizontally
 * scaled deployments would need a shared store. This service is intentionally isolated
 * so it can be replaced without touching controllers or filters.
 *
 * Bucket scope by tier:
 * <ul>
 *   <li><b>anonymous</b> — client IP only (no authenticated principal).</li>
 *   <li><b>authenticated</b> — authenticated principal identity when available, falling back
 *       to client IP when the security context has not yet been established.</li>
 *   <li><b>admin</b> — authenticated principal identity when available, with the same IP
 *       fallback. One administrator must never consume another administrator's budget just
 *       because they share a NAT/proxy.</li>
 *   <li><b>upload</b> — authenticated principal identity when available. Uploads are expensive,
 *       so subnets must not share one budget.</li>
 * </ul>
 *
 * When multiple tiers apply to one request (for example an admin uploading a document),
 * every applicable tier is checked and the strictest remaining window wins, so adding
 * tiers can never loosen an effective limit.
 *
 * Configuration lives in {@link RateLimitProperties} under {@code taloms.resiliency.*}.
 * Environment overrides use normalised kebab-case property names, e.g.
 * {@code TALOMS_RESILIENCY_AUTHENTICATED_REQUESTS-PER-MINUTE}.
 */
@Slf4j
@Service
public class HttpRequestRateLimitService {

    /** One fixed window per (tier, bucket key). */
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private final RateLimitProperties properties;

    public HttpRequestRateLimitService(RateLimitProperties properties) {
        this.properties = properties;
    }

    /**
     * Consumes one permit for the given tier/client, or denies with the number
     * of seconds until the window resets.
     */
    public Decision tryConsume(String tier, String bucketKey, int limit, long windowSeconds) {
        if (!properties.isEnabled()) {
            return Decision.allow();
        }
        long now = Instant.now().getEpochSecond();
        purgeIfOversized(now);

        Window window = windows.compute(bucketKey, (key, existing) -> {
            if (existing == null || now - existing.startEpochSecond() >= windowSeconds) {
                return new Window(now, new AtomicLong(0));
            }
            return existing;
        });

        long used = window.count().incrementAndGet();
        if (used > limit) {
            long retryAfter = Math.max(1, windowSeconds - (now - window.startEpochSecond()));
            log.warn("Rate limit exceeded [tier={}, client={}, limit={}, used={}, retryAfter={}]",
                    tier, maskClientId(bucketKey), limit, used, retryAfter);
            return Decision.deny(retryAfter);
        }
        return Decision.allow();
    }

    private void purgeIfOversized(long now) {
        if (windows.size() <= properties.getMaxTrackedClients()) {
            return;
        }
        windows.entrySet().removeIf(e -> now - e.getValue().startEpochSecond() >= 60);
    }

    /** Visible for testing — clears all counters. */
    public void reset() {
        windows.clear();
    }

    /**
     * Returns a masked representation suitable for logs. The filter owns the raw client IP
     * extraction, so this method receives the already-formed bucket key and avoids re-parsing
     * trusted-proxy headers here.
     */
    static String maskClientId(String bucketKey) {
        if (bucketKey == null) {
            return "-";
        }
        int idx = bucketKey.indexOf(':');
        if (idx < 0) {
            return bucketKey.length() > 16 ? bucketKey.substring(0, 16) : bucketKey;
        }
        String identity = bucketKey.substring(idx + 1);
        if (identity.length() <= 16) {
            return identity;
        }
        // Keep first + last fragments so ADMIN vs AUTH buckets are distinguishable without
        // storing full usernames.
        return identity.substring(0, 8) + "..." + identity.substring(identity.length() - 4);
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {

        public static Decision allow() {
            return new Decision(true, 0);
        }

        public static Decision deny(long retryAfterSeconds) {
            return new Decision(false, Math.max(1, retryAfterSeconds));
        }
    }

    private record Window(long startEpochSecond, AtomicLong count) {
    }
}
