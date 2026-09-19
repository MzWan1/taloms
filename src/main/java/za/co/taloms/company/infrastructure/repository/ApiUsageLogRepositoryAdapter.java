package za.co.taloms.company.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import za.co.taloms.company.domain.entity.ApiUsageLog;
import za.co.taloms.company.domain.repository.ApiUsageLogRepositoryPort;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class ApiUsageLogRepositoryAdapter implements ApiUsageLogRepositoryPort {

    private final ApiUsageLogJpaRepository jpaRepository;

    @Override
    public ApiUsageLog save(ApiUsageLog log) {
        return jpaRepository.save(log);
    }

    @Override
    public Page<ApiUsageLog> findByCompanyId(Long companyId, Pageable pageable) {
        return jpaRepository.findByCompanyIdOrderByRequestedAtDesc(companyId, pageable);
    }

    @Override
    public Page<ApiUsageLog> findByCompanyIdSince(Long companyId, LocalDateTime since, Pageable pageable) {
        return jpaRepository.findByCompanyIdAndRequestedAtAfterOrderByRequestedAtDesc(companyId, since, pageable);
    }

    @Override
    public long countByCompanyId(Long companyId) {
        return jpaRepository.countByCompanyId(companyId);
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }
}
