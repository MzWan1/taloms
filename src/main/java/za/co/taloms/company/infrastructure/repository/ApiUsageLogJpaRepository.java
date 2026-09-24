package za.co.taloms.company.infrastructure.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.taloms.company.domain.entity.ApiUsageLog;

import java.time.LocalDateTime;

public interface ApiUsageLogJpaRepository extends JpaRepository<ApiUsageLog, Long> {

    Page<ApiUsageLog> findByCompanyIdOrderByRequestedAtDesc(Long companyId, Pageable pageable);

    Page<ApiUsageLog> findByCompanyIdAndRequestedAtAfterOrderByRequestedAtDesc(
            Long companyId, LocalDateTime since, Pageable pageable);

    long countByCompanyId(Long companyId);

    long countByApiKeyId(Long apiKeyId);
}
