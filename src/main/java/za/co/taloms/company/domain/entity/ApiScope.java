package za.co.taloms.company.domain.entity;

/**
 * Permission (scope) attached to an API key. A key may hold several scopes.
 *
 * The authentication mechanism is scope-driven, so additional TALOMS APIs can
 * be exposed later by adding an enum constant and enforcing it on the endpoint —
 * no change to the authentication design is required.
 */
public enum ApiScope {
    /** Read/verify a resident's proof of residence. */
    POR_READ("Verify proof of residence"),

    /** Read the calling company's own API keys and usage records. */
    COMPANY_SELF_READ("Read own API keys and usage");

    private final String description;

    ApiScope(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /** Spring Security authority name used to enforce this scope. */
    public String authority() {
        return "SCOPE_" + name();
    }
}