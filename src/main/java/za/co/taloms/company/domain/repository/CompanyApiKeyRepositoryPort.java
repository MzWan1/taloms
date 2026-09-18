package za.co.taloms.company.domain.repository;

import za.co.taloms.company.domain.entity.CompanyApiKey;

import java.util.List;
import java.util.Optional;

public interface CompanyApiKeyRepositoryPort {

    CompanyApiKey save(CompanyApiKey apiKey);

    Optional<CompanyApiKey> findById(Long id);

    /** Primary API-authentication lookup (unique index on key_hash). */
    Optional<CompanyApiKey> findByKeyHash(String keyHash);

    List<CompanyApiKey> findByCompanyId(Long companyId);

    /** Locates a key by id, but only if it belongs to the given company. */
    Optional<CompanyApiKey> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByKeyHash(String keyHash);

    long countByCompanyIdAndStatus(Long companyId, za.co.taloms.company.domain.entity.ApiKeyStatus status);

    long countAll();
}