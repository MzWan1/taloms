package za.co.taloms.company.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.co.taloms.company.domain.entity.ApiUsageLog;

import java.time.LocalDateTime;

public interface ApiUsageLogRepositoryPort {

    ApiUsageLog save(ApiUsageLog log);

    Page<ApiUsageLog> findByCompanyId(Long companyId, Pageable pageable);

    Page<ApiUsageLog> findByCompanyIdSince(Long companyId, LocalDateTime since, Pageable pageable);

    long countByCompanyId(Long companyId);

    long countByApiKeyId(Long apiKeyId);

    long countAll();
}
