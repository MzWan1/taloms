package za.co.taloms.company.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the abuse-protection limits: per-key throughput and repeated lookups
 * of the same ID number.
 */
class ApiRateLimitServiceTest {

    private ApiRateLimitService service;

    @BeforeEach
    void setUp() {
        // 3 requests per window, 2 lookups of the same ID per window.
        service = new ApiRateLimitService(3, 2);
    }

    @Test
    void shouldAllowRequestsUpToTheLimitThenReject() {
        assertTrue(service.checkRequest(1L).allowed());
        assertTrue(service.checkRequest(1L).allowed());
        assertTrue(service.checkRequest(1L).allowed());

        var denied = service.checkRequest(1L);

        assertFalse(denied.allowed(), "requests beyond the limit must be rejected");
        assertTrue(denied.retryAfterSeconds() > 0, "a retry hint must be supplied");
    }

    @Test
    void shouldTrackLimitsPerApiKey() {
        service.checkRequest(1L);
        service.checkRequest(1L);
        service.checkRequest(1L);

        // A different key has its own budget.
        assertTrue(service.checkRequest(2L).allowed(),
                "one key exhausting its budget must not throttle another key");
    }

    @Test
    void shouldThrottleRepeatedQueriesOfTheSameIdNumber() {
        String hash = "a".repeat(64);

        assertTrue(service.checkIdNumberQuery(1L, hash).allowed());
        assertTrue(service.checkIdNumberQuery(1L, hash).allowed());
        assertFalse(service.checkIdNumberQuery(1L, hash).allowed(),
                "repeated lookups of the same ID must be throttled");
    }

    @Test
    void shouldTrackRepeatedIdQueriesPerIdAndPerKey() {
        String hashA = "a".repeat(64);
        String hashB = "b".repeat(64);

        service.checkIdNumberQuery(1L, hashA);
        service.checkIdNumberQuery(1L, hashA);

        assertTrue(service.checkIdNumberQuery(1L, hashB).allowed(),
                "a different ID number must not be affected");
        assertTrue(service.checkIdNumberQuery(2L, hashA).allowed(),
                "the same ID queried by a different key must not be affected");
    }

    @Test
    void shouldAllowQueryWhenNoIdHashIsAvailable() {
        assertTrue(service.checkIdNumberQuery(1L, null).allowed());
    }

    @Test
    void resetShouldClearAllCounters() {
        service.checkRequest(1L);
        service.checkRequest(1L);
        service.checkRequest(1L);
        assertFalse(service.checkRequest(1L).allowed());

        service.reset();

        assertTrue(service.checkRequest(1L).allowed());
    }
}