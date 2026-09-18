package za.co.taloms.company.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import za.co.taloms.company.domain.entity.ApiUsageLog;
import za.co.taloms.company.domain.repository.ApiUsageLogRepositoryPort;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ApiUsageLogRepositoryAdapter implements ApiUsageLogRepositoryPort {

    private final ApiUsageLogJpaRepository jpaRepository;

    @Override
    public ApiUsageLog save(ApiUsageLog log) {
        return jpaRepository.save(log);
    }

    @Override
    public List<ApiUsageLog> findAll() {
        return jpaRepository.findAll(Sort.by(Sort.Direction.DESC, "requestedAt"));
    }

    @Override
    public List<ApiUsageLog> findByCompanyId(Long companyId) {
        return jpaRepository.findByCompanyIdOrderByRequestedAtDesc(companyId);
    }

    @Override
    public List<ApiUsageLog> findByCompanyIdSince(Long companyId, LocalDateTime since) {
        return jpaRepository.findByCompanyIdAndRequestedAtAfterOrderByRequestedAtDesc(companyId, since);
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