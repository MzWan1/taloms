package za.co.taloms.company.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.company.application.dto.CompanyApiKeyCreateRequest;
import za.co.taloms.company.application.dto.CompanyApiKeyResponse;
import za.co.taloms.company.application.dto.CompanyApiKeySummaryResponse;
import za.co.taloms.company.domain.entity.ApiScope;
import za.co.taloms.company.domain.entity.Company;
import za.co.taloms.company.domain.entity.CompanyApiKey;
import za.co.taloms.company.domain.repository.CompanyApiKeyRepositoryPort;
import za.co.taloms.company.domain.repository.CompanyRepositoryPort;
import za.co.taloms.company.infrastructure.security.ApiKeyHasher;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CompanyApiKeyServiceImpl implements CompanyApiKeyService {

    private static final String ONE_TIME_NOTICE =
            "Store this API key securely. It is shown only once and cannot be retrieved again.";

    private final CompanyRepositoryPort companyRepository;
    private final CompanyApiKeyRepositoryPort apiKeyRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final za.co.taloms.company.domain.repository.ApiUsageLogRepositoryPort usageLogRepository;

    @Override
    public CompanyApiKeyResponse generateKey(Long companyId,
                                             CompanyApiKeyCreateRequest request,
                                             String actorUsername) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
        if (!company.isActive()) {
            throw new BusinessValidationException(
                    "API keys cannot be issued for a disabled company. Enable the company first.");
        }

        Set<ApiScope> scopes = resolveScopes(request);

        String rawKey = apiKeyHasher.generateRawKey();
        CompanyApiKey apiKey = CompanyApiKey.builder()
                .company(company)
                .label(blankToNull(request.getLabel()))
                .keyPrefix(apiKeyHasher.displayPrefix(rawKey))
                .keyHash(apiKeyHasher.hashApiKey(rawKey))
                .scopes(scopes)
                .createdBy(actorUsername)
                .build();

        CompanyApiKey saved = apiKeyRepository.save(apiKey);
        log.info("API key {} (prefix {}) issued for company '{}' (id {}) by {} with scopes {}",
                saved.getId(), saved.getKeyPrefix(), company.getName(), companyId, actorUsername, scopes);

        // The raw key is returned here and nowhere else.
        return CompanyApiKeyResponse.builder()
                .id(saved.getId())
                .companyId(companyId)
                .label(saved.getLabel())
                .keyPrefix(saved.getKeyPrefix())
                .scopes(saved.getScopes())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .apiKey(rawKey)
                .notice(ONE_TIME_NOTICE)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyApiKeySummaryResponse> findByCompany(Long companyId) {
        if (companyRepository.findById(companyId).isEmpty()) {
            throw new ResourceNotFoundException("Company", companyId);
        }
        return apiKeyRepository.findByCompanyId(companyId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public CompanyApiKeySummaryResponse revokeKey(Long companyId,
                                                  Long keyId,
                                                  String reason,
                                                  String actorUsername) {
        // Scoped lookup: a key can only be revoked through its owning company.
        CompanyApiKey apiKey = apiKeyRepository.findByIdAndCompanyId(keyId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("API key", keyId));
        if (!apiKey.isActive()) {
            throw new BusinessValidationException("This API key is already revoked.");
        }
        apiKey.revoke(actorUsername, blankToNull(reason));
        CompanyApiKey saved = apiKeyRepository.save(apiKey);
        log.warn("API key {} (prefix {}) REVOKED for company id {} by {} (reason: {})",
                saved.getId(), saved.getKeyPrefix(), companyId, actorUsername, reason);
        return toSummary(saved);
    }

    private Set<ApiScope> resolveScopes(CompanyApiKeyCreateRequest request) {
        if (request.getScopes() == null || request.getScopes().isEmpty()) {
            return new LinkedHashSet<>(Set.of(ApiScope.POR_READ));
        }
        return new LinkedHashSet<>(request.getScopes());
    }

    private CompanyApiKeySummaryResponse toSummary(CompanyApiKey apiKey) {
        long totalRequests = apiKey.getId() != null ? usageLogRepository.countByApiKeyId(apiKey.getId()) : 0L;
        return CompanyApiKeySummaryResponse.builder()
                .id(apiKey.getId())
                .companyId(apiKey.getCompany() == null ? null : apiKey.getCompany().getId())
                .label(apiKey.getLabel())
                .keyPrefix(apiKey.getKeyPrefix())
                .scopes(apiKey.getScopes())
                .status(apiKey.getStatus())
                .statusDisplay(apiKey.getStatus() == null ? null : apiKey.getStatus().getDisplayName())
                .statusBadgeClass(apiKey.getStatus() == null ? null : apiKey.getStatus().getBadgeClass())
                .lastUsedAt(apiKey.getLastUsedAt())
                .revokedBy(apiKey.getRevokedBy())
                .revokeReason(apiKey.getRevokeReason())
                .revokedAt(apiKey.getRevokedAt())
                .createdBy(apiKey.getCreatedBy())
                .createdAt(apiKey.getCreatedAt())
                .totalRequests(totalRequests)
                .build();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}