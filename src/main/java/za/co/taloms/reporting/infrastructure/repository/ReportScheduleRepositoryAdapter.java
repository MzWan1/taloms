package za.co.taloms.reporting.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.reporting.domain.entity.ReportSchedule;
import za.co.taloms.reporting.domain.entity.ReportType;
import za.co.taloms.reporting.domain.repository.ReportScheduleRepositoryPort;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportScheduleRepositoryAdapter implements ReportScheduleRepositoryPort {

    private final ReportScheduleJpaRepository jpaRepository;

    @Override
    public ReportSchedule save(ReportSchedule schedule) {
        return jpaRepository.save(schedule);
    }

    @Override
    public Optional<ReportSchedule> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ReportSchedule> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<ReportSchedule> findByActiveTrue() {
        return jpaRepository.findByActiveTrue();
    }

    @Override
    public List<ReportSchedule> findByReportType(ReportType reportType) {
        return jpaRepository.findByReportType(reportType);
    }

    @Override
    public List<ReportSchedule> findActiveByReportType(ReportType reportType) {
        return jpaRepository.findByActiveTrueAndReportType(reportType);
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }
}