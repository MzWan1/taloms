package za.co.taloms.reporting.domain.repository;

import za.co.taloms.reporting.domain.entity.ReportSchedule;
import za.co.taloms.reporting.domain.entity.ReportType;
import java.util.List;
import java.util.Optional;

public interface ReportScheduleRepositoryPort {
    ReportSchedule save(ReportSchedule schedule);
    Optional<ReportSchedule> findById(Long id);
    List<ReportSchedule> findAll();
    List<ReportSchedule> findByActiveTrue();
    List<ReportSchedule> findByReportType(ReportType reportType);
    List<ReportSchedule> findActiveByReportType(ReportType reportType);
    long countAll();
}