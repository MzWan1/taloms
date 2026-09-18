package za.co.taloms.company.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.company.application.dto.CompanyApiAuthResult;
import za.co.taloms.company.domain.entity.ApiFailureReason;
import za.co.taloms.company.domain.entity.ApiScope;
import za.co.taloms.company.domain.entity.Company;
import za.co.taloms.company.domain.entity.CompanyApiKey;
import za.co.taloms.company.domain.repository.CompanyApiKeyRepositoryPort;
import za.co.taloms.company.domain.repository.CompanyRepositoryPort;
import za.co.taloms.company.domain.security.CompanyApiPrincipal;
import za.co.taloms.company.infrastructure.security.ApiKeyHasher;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

/**
 * Dedicated authentication mechanism for company API requests.
 *
 * This is intentionally independent of the TALOMS browser/JWT/session
 * authentication: a company proves its identity with an API key, not a user
 * session. Verification order mirrors the access-control rules:
 *
 *   1. key supplied?
 *   2. key exists (looked up by SHA-256 digest — the raw key is never stored)?
 *   3. key is ACTIVE (not revoked)?
 *   4. owning company exists and is ACTIVE?
 *
 * Scope enforcement happens later, at the endpoint (authorization), so that a
 * valid key without the required scope is distinguished from a bad key.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyApiAuthenticationService {

    /** Avoids a database write on every single API call. */
    private static final Duration LAST_USED_WRITE_INTERVAL = Duration.ofMinutes(5);

    private final CompanyApiKeyRepositoryPort apiKeyRepository;
    private final CompanyRepositoryPort companyRepository;
    private final ApiKeyHasher apiKeyHasher;

    @Transactional
    public CompanyApiAuthResult authenticate(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            return CompanyApiAuthResult.failure(ApiFailureReason.MISSING_API_KEY);
        }

        // The raw key is hashed and matched against the stored digest. The
        // lookup is served by a unique index, so it does not leak whether a
        // particular key exists.
        String keyHash = apiKeyHasher.hashApiKey(rawApiKey.trim());
        CompanyApiKey apiKey = apiKeyRepository.findByKeyHash(keyHash).orElse(null);
        if (apiKey == null) {
            return CompanyApiAuthResult.failure(ApiFailureReason.INVALID_API_KEY);
        }
        if (!apiKey.isActive()) {
            return CompanyApiAuthResult.failure(
                    ApiFailureReason.REVOKED_API_KEY,
                    apiKey.getCompany() == null ? null : apiKey.getCompany().getId(),
                    apiKey.getId());
        }

        Company company = apiKey.getCompany() == null
                ? null
                : companyRepository.findById(apiKey.getCompany().getId()).orElse(null);
        if (company == null || !company.isActive()) {
            return CompanyApiAuthResult.failure(
                    ApiFailureReason.DISABLED_COMPANY,
                    apiKey.getCompany() == null ? null : apiKey.getCompany().getId(),
                    apiKey.getId());
        }

        Set<ApiScope> scopes = apiKey.getScopes() == null
                ? Collections.emptySet()
                : apiKey.getScopes();

        touchLastUsed(apiKey);

        return CompanyApiAuthResult.success(new CompanyApiPrincipal(
                company.getId(), company.getName(), apiKey.getId(), scopes));
    }

    private void touchLastUsed(CompanyApiKey apiKey) {
        LocalDateTime now = LocalDateTime.now();
        if (apiKey.getLastUsedAt() != null
                && apiKey.getLastUsedAt().isAfter(now.minus(LAST_USED_WRITE_INTERVAL))) {
            return;
        }
        apiKey.touchLastUsed();
        apiKeyRepository.save(apiKey);
    }
}