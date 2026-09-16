package za.co.taloms.company.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.taloms.company.domain.entity.ApiKeyStatus;
import za.co.taloms.company.domain.entity.CompanyApiKey;

import java.util.List;
import java.util.Optional;

public interface CompanyApiKeyJpaRepository extends JpaRepository<CompanyApiKey, Long> {

    Optional<CompanyApiKey> findByKeyHash(String keyHash);

    boolean existsByKeyHash(String keyHash);

    List<CompanyApiKey> findByCompany_IdOrderByCreatedAtDesc(Long companyId);

    Optional<CompanyApiKey> findByIdAndCompany_Id(Long id, Long companyId);

    long countByCompany_IdAndStatus(Long companyId, ApiKeyStatus status);
}