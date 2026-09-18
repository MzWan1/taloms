package za.co.taloms.company.application.dto;

/**
 * Result of a rate-limit check. {@code retryAfterSeconds} is surfaced to the
 * caller as the standard {@code Retry-After} header.
 */
public record RateLimitDecision(boolean allowed, long retryAfterSeconds) {

    private static final RateLimitDecision ALLOWED = new RateLimitDecision(true, 0);

    public static RateLimitDecision allow() {
        return ALLOWED;
    }

    public static RateLimitDecision deny(long retryAfterSeconds) {
        return new RateLimitDecision(false, Math.max(1, retryAfterSeconds));
    }
}