package za.co.taloms.company.domain.repository;

import za.co.taloms.company.domain.entity.ApiUsageLog;

import java.time.LocalDateTime;
import java.util.List;

public interface ApiUsageLogRepositoryPort {

    ApiUsageLog save(ApiUsageLog log);

    List<ApiUsageLog> findAll();

    List<ApiUsageLog> findByCompanyId(Long companyId);

    List<ApiUsageLog> findByCompanyIdSince(Long companyId, LocalDateTime since);

    long countByCompanyId(Long companyId);

    long countAll();
}