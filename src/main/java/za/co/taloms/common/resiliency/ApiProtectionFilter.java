package za.co.taloms.common.resiliency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.common.resiliency.RateLimitProperties.Tier;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cross-cutting API protection filter for all non-company traffic.
 *
 * The external company API is intentionally EXCLUDED: /api/external/** already
 * has its own per-API-key limiter ({@code CompanyApiKeyAuthenticationFilter}),
 * which is keyed per company rather than per IP.
 *
 * Responsibilities, in order:
 *   1. attach/propagate an X-Request-Id (correlation ID for log diagnosis);
 *   2. enforce a request-body size cap for upload endpoints;
 *   3. enforce tiered per-IP rate limits with 429 + Retry-After.
 *
 * Tiers (see {@link RateLimitProperties}): ADMIN, AUTHENTICATED, ANONYMOUS,
 * UPLOAD. When several limits apply (e.g. an admin uploading), ALL of them are
 * enforced and the most restrictive remaining window wins, so enabling more
 * tiers can never loosen an effective limit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiProtectionFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTRIBUTE = "taloms.requestId";

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** Document + PTO document/upload endpoints get the tightest budget. */
    private static final String[] UPLOAD_PATTERNS = {
            "/api/documents/upload",
            "/api/ptos/*/documents",
            "/api/ptos/*/id-documents",
            "/api/ptos/*/allocation-letter",
            "/api/ptos/*/site-sketch"
    };

    private final HttpRequestRateLimitService rateLimitService;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = resolveRequestId(request);
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        String path = request.getServletPath();
        String method = request.getMethod();
        boolean isOptions = "OPTIONS".equalsIgnoreCase(method);

        if (isOptions) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/error") || path.startsWith("/actuator") || path.startsWith("/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/api/external")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = clientIp(request);
        boolean authenticated = isAuthenticated(request);
        AuthenticationTier primaryTier = chooseTierForRequest(authenticated, path);
        if (primaryTier == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isUploadPath(path)
                && request.getContentLengthLong() > properties.getUploadLimits().getMaxBytes()) {
            writeError(request, response, requestId, HttpStatus.PAYLOAD_TOO_LARGE,
                    "File exceeds the maximum allowed size of "
                            + (properties.getUploadLimits().getMaxBytes() / (1024 * 1024)) + "MB.",
                    0);
            return;
        }

        RateLimitDecision decision = evaluateTiers(requestId, path, method, clientIp, authenticated,
                primaryTier);

        if (!decision.allowed()) {
            writeRateLimitedResponse(request, response, requestId, decision.retryAfterSeconds(),
                    primaryTier.code());
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (log.isTraceEnabled()) {
                log.trace("{} {} [requestId={}, status={}]", request.getMethod(),
                        path, requestId, response.getStatus());
            }
        }
    }

    /** Visible for testing. */
    RateLimitDecision checkRateLimits(HttpServletRequest request, String path, String clientIp) {
        AtomicLong minRetryAfter = new AtomicLong(-1);

        if (isUploadPath(path)) {
            applyTier("upload", properties.getUploadLimits().getRate(), clientIp, minRetryAfter);
        }
        if (request.isUserInRole("ADMIN")) {
            applyTier("admin", properties.getAdmin(), clientIp, minRetryAfter);
        } else if (isAuthenticated(request)) {
            applyTier("auth", properties.getAuthenticated(), clientIp, minRetryAfter);
        } else {
            applyTier("anon", properties.getAnonymous(), clientIp, minRetryAfter);
        }

        if (minRetryAfter.get() >= 0) {
            return RateLimitDecision.deny(minRetryAfter.get());
        }
        return RateLimitDecision.allow();
    }

    private boolean isAuthenticated(HttpServletRequest request) {
        return request.getRemoteUser() != null;
    }

    private void applyTier(String tier, Tier tierConfig, String clientIp,
                           AtomicLong minRetryAfter) {
        if (tierConfig == null || !tierConfig.isEnabled()) {
            return;
        }
        var decision = rateLimitService.tryConsume(tier, clientIp, tierConfig.getRequestsPerMinute());
        if (!decision.allowed()) {
            minRetryAfter.accumulateAndGet(decision.retryAfterSeconds(),
                    (current, candidate) -> current < 0 ? candidate : Math.min(current, candidate));
            log.warn("Rate limit denial [tier={}, client={}]", tier, clientIp);
        }
    }

    private boolean isUploadPath(String path) {
        for (String pattern : UPLOAD_PATTERNS) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        if (incoming != null && !incoming.isBlank() && incoming.length() <= 64) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }

    /** Prefers the first X-Forwarded-For hop (Render terminates TLS in front). */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response,
                            String requestId, HttpStatus status, String message,
                            long retryAfterSeconds) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        log.warn("Rejected request {} {} [status={}, requestId={}]",
                request.getMethod(), request.getServletPath(), status.value(), requestId);
        response.setStatus(status.value());
        response.setHeader(REQUEST_ID_HEADER, requestId);
        if (retryAfterSeconds > 0) {
            response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message)));
    }

    record RateLimitDecision(boolean allowed, long retryAfterSeconds) {

        static RateLimitDecision allow() {
            return new RateLimitDecision(true, 0);
        }

        static RateLimitDecision deny(long retryAfterSeconds) {
            return new RateLimitDecision(false, retryAfterSeconds);
        }
    }
}

