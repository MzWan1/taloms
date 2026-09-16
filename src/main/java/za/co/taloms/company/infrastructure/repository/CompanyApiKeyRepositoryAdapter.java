package za.co.taloms.company.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.company.domain.entity.ApiKeyStatus;
import za.co.taloms.company.domain.entity.CompanyApiKey;
import za.co.taloms.company.domain.repository.CompanyApiKeyRepositoryPort;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CompanyApiKeyRepositoryAdapter implements CompanyApiKeyRepositoryPort {

    private final CompanyApiKeyJpaRepository jpaRepository;

    @Override
    public CompanyApiKey save(CompanyApiKey apiKey) {
        return jpaRepository.save(apiKey);
    }

    @Override
    public Optional<CompanyApiKey> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<CompanyApiKey> findByKeyHash(String keyHash) {
        return jpaRepository.findByKeyHash(keyHash);
    }

    @Override
    public List<CompanyApiKey> findByCompanyId(Long companyId) {
        return jpaRepository.findByCompany_IdOrderByCreatedAtDesc(companyId);
    }

    @Override
    public Optional<CompanyApiKey> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId);
    }

    @Override
    public boolean existsByKeyHash(String keyHash) {
        return jpaRepository.existsByKeyHash(keyHash);
    }

    @Override
    public long countByCompanyIdAndStatus(Long companyId, ApiKeyStatus status) {
        return jpaRepository.countByCompany_IdAndStatus(companyId, status);
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }
}