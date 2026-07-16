package za.co.taloms.reporting.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.taloms.reporting.domain.entity.ReportSchedule;
import za.co.taloms.reporting.domain.entity.ReportType;
import java.util.List;

public interface ReportScheduleJpaRepository extends JpaRepository<ReportSchedule, Long> {

    List<ReportSchedule> findByActiveTrue();

    List<ReportSchedule> findByReportType(ReportType reportType);

    List<ReportSchedule> findByActiveTrueAndReportType(ReportType reportType);
}