package za.co.taloms.company.presentation;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.common.Roles;
import za.co.taloms.company.application.dto.ApiUsageRecord;
import za.co.taloms.company.application.dto.CompanyApiAuthResult;
import za.co.taloms.company.application.dto.RateLimitDecision;
import za.co.taloms.company.application.service.ApiRateLimitService;
import za.co.taloms.company.application.service.ApiUsageService;
import za.co.taloms.company.application.service.CompanyApiAuthenticationService;
import za.co.taloms.company.domain.entity.ApiFailureReason;
import za.co.taloms.company.domain.entity.ApiOutcome;
import za.co.taloms.company.domain.entity.ApiScope;
import za.co.taloms.company.domain.security.CompanyApiPrincipal;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Dedicated authentication filter for the external company API.
 *
 * It applies ONLY to {@code /api/external/**} and is completely separate from the
 * TALOMS browser/JWT/session authentication: a company proves its identity with
 * an API key, never a user session. Any session that happens to be present is
 * discarded for these paths.
 *
 * Per request it:
 *   1. extracts the key from the {@code X-API-Key} header,
 *   2. authenticates it (exists, ACTIVE, owning company ACTIVE),
 *   3. applies a per-key rate limit,
 *   4. publishes a {@code ROLE_COMPANY} + {@code SCOPE_} authentication,
 *   5. writes exactly one usage record to {@code api_usage_logs}.
 *
 * Every failure produces the same generic 401/429 response so the API never
 * reveals whether a key, company or resident exists.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyApiKeyAuthenticationFilter extends OncePerRequestFilter {

    /** Header carrying the company API key. */
    public static final String API_KEY_HEADER = "X-API-Key";

    /** Request path prefix served by the partner API. */
    public static final String API_PATH_PREFIX = "/api/external";

    /**
     * Request attribute set by the PoR endpoint holding the SHA-256 digest of the
     * queried ID number (never the raw ID) so the usage record can correlate
     * lookups without storing personal information.
     */
    public static final String ATTR_ID_NUMBER_HASH = "taloms.company.api.idNumberHash";

    /** Scope required by each exposed endpoint (used for usage logging). */
    private static final Map<String, ApiScope> REQUIRED_SCOPES = new LinkedHashMap<>();

    static {
        REQUIRED_SCOPES.put("/api/external/por/verify", ApiScope.POR_READ);
        REQUIRED_SCOPES.put("/api/external/self/company", ApiScope.COMPANY_SELF_READ);
        REQUIRED_SCOPES.put("/api/external/self/keys", ApiScope.COMPANY_SELF_READ);
        REQUIRED_SCOPES.put("/api/external/self/usage", ApiScope.COMPANY_SELF_READ);
    }
private final CompanyApiAuthenticationService authenticationService;
    private final ApiRateLimitService rateLimitService;
    private final ApiUsageService usageService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getServletPath().startsWith(API_PATH_PREFIX);
    }

@Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // A browser session must never grant access to the partner API.
        SecurityContextHolder.clearContext();

        String rawApiKey = request.getHeader(API_KEY_HEADER);
        CompanyApiAuthResult result = authenticationService.authenticate(rawApiKey);

        if (!result.isAuthenticated()) {
            record(request, result.getCompanyId(), result.getApiKeyId(), null,
                    ApiOutcome.FAILURE, HttpStatus.UNAUTHORIZED.value(),
                    result.getFailureReason(), null, 0);
            writeError(response, HttpStatus.UNAUTHORIZED, "API authentication failed.", 0);
            return;
        }

        CompanyApiPrincipal principal = result.getPrincipal();

        // Abuse protection: per-key throughput before any resident lookup.
        RateLimitDecision decision = rateLimitService.checkRequest(principal.apiKeyId());
        if (!decision.allowed()) {
            record(request, principal.companyId(), principal.apiKeyId(),
                    requiredScope(request), ApiOutcome.FAILURE,
                    HttpStatus.TOO_MANY_REQUESTS.value(), ApiFailureReason.RATE_LIMITED, null, 0);
            writeError(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests. Please retry later.", decision.retryAfterSeconds());
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(authenticate(principal, request));

        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            String idNumberHash = (String) request.getAttribute(ATTR_ID_NUMBER_HASH);
            boolean succeeded = status < HttpStatus.BAD_REQUEST.value();
            record(request, principal.companyId(), principal.apiKeyId(), requiredScope(request),
                    succeeded ? ApiOutcome.SUCCESS : ApiOutcome.FAILURE,
                    status,
                    succeeded ? null : ApiFailureReason.fromHttpStatus(status),
                    idNumberHash,
                    elapsedMs(startedAt));
        }
    }

    /** Grants {@code ROLE_COMPANY} plus one {@code SCOPE_*} authority per key scope. */
    private UsernamePasswordAuthenticationToken authenticate(CompanyApiPrincipal principal,
                                                            HttpServletRequest request) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority(Roles.COMPANY));
        if (principal.scopes() != null) {
            principal.scopes().forEach(scope ->
                    authorities.add(new SimpleGrantedAuthority(scope.authority())));
        }
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authentication;
    }

    private String requiredScope(HttpServletRequest request) {
        ApiScope scope = REQUIRED_SCOPES.get(request.getServletPath());
        return scope == null ? null : scope.name();
    }

    private void record(HttpServletRequest request,
                        Long companyId,
                        Long apiKeyId,
                        String requiredScope,
                        ApiOutcome outcome,
                        int status,
                        ApiFailureReason failureReason,
                        String idNumberHash,
                        int durationMs) {
        usageService.record(ApiUsageRecord.builder()
                .companyId(companyId)
                .apiKeyId(apiKeyId)
                .endpoint(request.getServletPath())
                .httpMethod(request.getMethod())
                .requiredScope(requiredScope)
                .outcome(outcome)
                .responseStatus(status)
                .failureReason(failureReason)
                .idNumberHash(idNumberHash)
                .clientIp(clientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .durationMs(durationMs)
                .build());
    }

    private void writeError(HttpServletResponse response,
                            HttpStatus status,
                            String message,
                            long retryAfterSeconds) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        if (retryAfterSeconds > 0) {
            response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        }
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message)));
    }

    /** Prefers the first X-Forwarded-For hop, falling back to the socket address. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private int elapsedMs(long startedAtNanos) {
        return (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }
}