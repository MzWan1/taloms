package za.co.taloms.company.domain.security;

import za.co.taloms.company.domain.entity.ApiScope;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The authenticated identity of an external company for the duration of a single
 * API request. Deliberately NOT a TALOMS {@code User}: a company authenticates
 * with an API key and can only ever see its own data.
 *
 * @param companyId   the owning company id
 * @param companyName the owning company name (for logging, never returned to others)
 * @param apiKeyId    the id of the API key that authenticated the request
 * @param scopes      the permissions granted to that key
 */
public record CompanyApiPrincipal(
        Long companyId,
        String companyName,
        Long apiKeyId,
        Set<ApiScope> scopes) {

    public CompanyApiPrincipal {
        scopes = scopes == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(scopes));
    }

    public boolean hasScope(ApiScope scope) {
        return scope != null && scopes.contains(scope);
    }

    /** Identity used by Spring Security for this request. */
    public String name() {
        return "company:" + companyId + ":key:" + apiKeyId;
    }
}