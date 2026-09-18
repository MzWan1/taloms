package za.co.taloms.company.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.taloms.company.domain.entity.ApiUsageLog;

import java.time.LocalDateTime;
import java.util.List;

public interface ApiUsageLogJpaRepository extends JpaRepository<ApiUsageLog, Long> {

    List<ApiUsageLog> findByCompanyIdOrderByRequestedAtDesc(Long companyId);

    List<ApiUsageLog> findByCompanyIdAndRequestedAtAfterOrderByRequestedAtDesc(
            Long companyId, LocalDateTime since);

    long countByCompanyId(Long companyId);
}