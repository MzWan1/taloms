package za.co.taloms.reporting.application.dto;

import lombok.*;
import za.co.taloms.reporting.domain.entity.ReportFormat;
import za.co.taloms.reporting.domain.entity.ReportType;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {

    private ReportType reportType;

    @Builder.Default
    private ReportFormat format = ReportFormat.PDF;  // Default to PDF

    private Long villageId;
    private Long authorityId;
    private String status;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String[] columns;
}