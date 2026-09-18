package za.co.taloms.company.domain.entity;

/**
 * Sanitised reason an external API request failed.
 *
 * These values are written to {@code api_usage_logs} for administrator
 * diagnostics. They are deliberately coarse (e.g. {@link #INVALID_API_KEY}) so
 * the usage log never records whether a specific key, company or resident exists.
 */
public enum ApiFailureReason {
    MISSING_API_KEY,
    INVALID_API_KEY,
    REVOKED_API_KEY,
    DISABLED_COMPANY,
    MISSING_SCOPE,
    RATE_LIMITED,
    VALIDATION_ERROR,
    NOT_FOUND,
    INTERNAL_ERROR;

    /** Value stored against the "required_scope" column is a scope name, not this. */
    public static ApiFailureReason fromHttpStatus(int status) {
        return switch (status) {
            case 400, 422      -> VALIDATION_ERROR;
            case 401           -> INVALID_API_KEY;
            case 403           -> MISSING_SCOPE;
            case 404           -> NOT_FOUND;
            case 429           -> RATE_LIMITED;
            default            -> status >= 500 ? INTERNAL_ERROR : NOT_FOUND;
        };
    }
}