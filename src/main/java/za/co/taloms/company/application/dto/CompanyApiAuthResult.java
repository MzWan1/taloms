package za.co.taloms.company.application.dto;

import lombok.*;
import za.co.taloms.company.domain.entity.ApiFailureReason;
import za.co.taloms.company.domain.security.CompanyApiPrincipal;

/**
 * Outcome of validating a company API key.
 *
 * A single generic failure is surfaced to the caller; the precise
 * {@link ApiFailureReason} is retained only for internal usage logging so the
 * HTTP response never reveals whether a key or company exists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyApiAuthResult {

    private CompanyApiPrincipal principal;
    private ApiFailureReason failureReason;

    /**
     * Populated when the key/company could be identified even though the request
     * was rejected (e.g. revoked key, disabled company) so administrators can see
     * failed attempts against a known company. Null for a missing/unknown key.
     */
    private Long companyId;
    private Long apiKeyId;

    public boolean isAuthenticated() {
        return principal != null;
    }

    public static CompanyApiAuthResult success(CompanyApiPrincipal principal) {
        return CompanyApiAuthResult.builder()
                .principal(principal)
                .companyId(principal.companyId())
                .apiKeyId(principal.apiKeyId())
                .build();
    }

    public static CompanyApiAuthResult failure(ApiFailureReason reason) {
        return CompanyApiAuthResult.builder().failureReason(reason).build();
    }

    public static CompanyApiAuthResult failure(ApiFailureReason reason, Long companyId, Long apiKeyId) {
        return CompanyApiAuthResult.builder()
                .failureReason(reason)
                .companyId(companyId)
                .apiKeyId(apiKeyId)
                .build();
    }
}