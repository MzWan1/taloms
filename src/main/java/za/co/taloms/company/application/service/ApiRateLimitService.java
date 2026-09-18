package za.co.taloms.company.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.co.taloms.company.application.dto.RateLimitDecision;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Smallest appropriate abuse-protection mechanism for the external API.
 *
 * TALOMS had no rate-limiting infrastructure before this feature, so rather than
 * adding a dependency (Bucket4j / Resilience4j) this in-memory fixed-window
 * limiter covers the two required controls:
 *
 *   1. per API-key request throughput ({@link #checkRequest}), and
 *   2. per API-key repeated ID-number lookups ({@link #checkIdNumberQuery}),
 *      which throttles enumeration of residents through repeated queries.
 *
 * Decision/limitation: state is per JVM instance, so a horizontally scaled
 * deployment would need a shared store. It is deliberately isolated behind this
 * single service so it can be swapped for a distributed limiter later without
 * touching authentication or controllers.
 */
@Slf4j
@Service
public class ApiRateLimitService {

    private static final int MAX_TRACKED_WINDOWS = 10_000;

    private final int requestsPerMinute;
    private final int idNumberQueriesPerHour;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public ApiRateLimitService(
            @Value("${taloms.company-api.rate-limit.requests-per-minute:60}") int requestsPerMinute,
            @Value("${taloms.company-api.rate-limit.id-queries-per-hour:60}") int idNumberQueriesPerHour) {
        this.requestsPerMinute = Math.max(1, requestsPerMinute);
        this.idNumberQueriesPerHour = Math.max(1, idNumberQueriesPerHour);
    }

    /** General throughput limit applied to every authenticated API request. */
    public RateLimitDecision checkRequest(Long apiKeyId) {
        return consume("req:" + apiKeyId, requestsPerMinute, 60);
    }

    /** Throttles repeated lookups of the same ID number by the same key. */
    public RateLimitDecision checkIdNumberQuery(Long apiKeyId, String idNumberHash) {
        if (idNumberHash == null) {
            return RateLimitDecision.allow();
        }
        return consume("id:" + apiKeyId + ":" + idNumberHash, idNumberQueriesPerHour, 3600);
    }

    private RateLimitDecision consume(String bucketKey, int limit, long windowSeconds) {
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
            log.warn("Company API rate limit exceeded for bucket {} ({} > {})", bucketKey, used, limit);
            return RateLimitDecision.deny(retryAfter);
        }
        return RateLimitDecision.allow();
    }

    private void purgeIfOversized(long now) {
        if (windows.size() <= MAX_TRACKED_WINDOWS) {
            return;
        }
        windows.entrySet().removeIf(e -> now - e.getValue().startEpochSecond() >= 3600);
    }

    /** Visible for testing — clears all counters. */
    public void reset() {
        windows.clear();
    }

    private record Window(long startEpochSecond, AtomicLong count) {
    }
}