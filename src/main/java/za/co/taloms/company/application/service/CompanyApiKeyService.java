package za.co.taloms.company.application.service;

import za.co.taloms.company.application.dto.CompanyApiKeyCreateRequest;
import za.co.taloms.company.application.dto.CompanyApiKeyResponse;
import za.co.taloms.company.application.dto.CompanyApiKeySummaryResponse;

import java.util.List;

/**
 * API-key management for external companies.
 *
 * Generation and revocation are ADMIN-only actions. Read operations are scoped
 * to a single company, which is what prevents one company from ever seeing
 * another company's credentials.
 */
public interface CompanyApiKeyService {

    /** Generates a key and returns the raw value exactly once. */
    CompanyApiKeyResponse generateKey(Long companyId, CompanyApiKeyCreateRequest request, String actorUsername);

    /** Keys belonging to a company, without any secret material. */
    List<CompanyApiKeySummaryResponse> findByCompany(Long companyId);

    /** Revokes a key. The key must belong to the given company. */
    CompanyApiKeySummaryResponse revokeKey(Long companyId, Long keyId, String reason, String actorUsername);
}