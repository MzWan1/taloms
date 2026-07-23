package za.co.taloms.reporting.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.reporting.application.dto.ReportRequest;
import za.co.taloms.reporting.application.dto.ReportResponse;
import za.co.taloms.reporting.application.service.ReportService;
import za.co.taloms.reporting.application.service.ReportServiceExcelExporter;
import za.co.taloms.reporting.application.service.ReportServicePdfExporter;
import za.co.taloms.reporting.domain.entity.ReportFormat;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportRestController {

    private final ReportServicePdfExporter pdfExporter;
    private final ReportServiceExcelExporter excelExporter;

    @PostMapping("/pto")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<byte[]> generatePTOReport(@RequestBody ReportRequest request) {
        log.info("Generating PTO Occupancy Register Report - Format: {}", request.getFormat());
        return buildResponse(getExporter(request.getFormat()).generatePtoOccupancyRegisterReport(request));
    }

    @PostMapping("/parcel")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<byte[]> generateLandParcelReport(@RequestBody ReportRequest request) {
        log.info("Generating Land Parcel Utilisation Report - Format: {}", request.getFormat());
        return buildResponse(getExporter(request.getFormat()).generateLandParcelUtilisationReport(request));
    }

    @PostMapping("/stand-allocation")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<byte[]> generateStandAllocationReport(@RequestBody ReportRequest request) {
        log.info("Generating Stand Allocation Report - Format: {}", request.getFormat());
        return buildResponse(getExporter(request.getFormat()).generateStandAllocationReport(request));
    }

    @PostMapping("/village-population")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<byte[]> generateVillagePopulationReport(@RequestBody ReportRequest request) {
        log.info("Generating Village Population Report - Format: {}", request.getFormat());
        return buildResponse(getExporter(request.getFormat()).generateVillagePopulationReport(request));
    }

    @PostMapping("/household-register")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<byte[]> generateHouseholdRegisterReport(@RequestBody ReportRequest request) {
        log.info("Generating Household Register Report - Format: {}", request.getFormat());
        return buildResponse(getExporter(request.getFormat()).generateHouseholdRegisterReport(request));
    }

    @PostMapping("/resident-demographics")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<byte[]> generateResidentDemographicsReport(@RequestBody ReportRequest request) {
        log.info("Generating Resident Demographics Report - Format: {}", request.getFormat());
        return buildResponse(getExporter(request.getFormat()).generateResidentDemographicsReport(request));
    }

    @PostMapping("/business-occupancy")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<byte[]> generateBusinessOccupancyReport(@RequestBody ReportRequest request) {
        log.info("Generating Business Occupancy Report - Format: {}", request.getFormat());
        return buildResponse(getExporter(request.getFormat()).generateBusinessOccupancyReport(request));
    }

    @PostMapping("/economic-activity")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<byte[]> generateEconomicActivityReport(@RequestBody ReportRequest request) {
        log.info("Generating Economic Activity Report - Format: {}", request.getFormat());
        return buildResponse(getExporter(request.getFormat()).generateEconomicActivityReport(request));
    }

    @PostMapping("/user-activity-audit")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<byte[]> generateUserActivityAuditReport(@RequestBody ReportRequest request) {
        log.info("Generating User Activity Audit Report - Format: {}", request.getFormat());
        return buildResponse(getExporter(request.getFormat()).generateUserActivityAuditReport(request));
    }

    @PostMapping("/document-management")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<byte[]> generateDocumentManagementReport(@RequestBody ReportRequest request) {
        log.info("Generating Document Management Report - Format: {}", request.getFormat());
        return buildResponse(getExporter(request.getFormat()).generateDocumentManagementReport(request));
    }

    @PostMapping("/performance-dashboard")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<byte[]> generatePerformanceDashboardReport(@RequestBody ReportRequest request) {
        log.info("Generating Performance Dashboard Report - Format: {}", request.getFormat());
        return buildResponse(getExporter(request.getFormat()).generatePerformanceDashboardReport(request));
    }

    @PostMapping("/land-boundary")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<byte[]> generateLandBoundaryReport(@RequestBody ReportRequest request) {
        log.info("Generating Land Boundary Report - Format: {}", request.getFormat());
        return buildResponse(getExporter(request.getFormat()).generateLandBoundaryReport(request));
    }

    private ReportService getExporter(ReportFormat format) {
        log.info("Selected exporter for format: {}", format);
        if (format == ReportFormat.EXCEL) {
            return excelExporter;
        }
        return pdfExporter;
    }

    private ResponseEntity<byte[]> buildResponse(ReportResponse response) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(response.getContentType()));
        headers.setContentDispositionFormData("attachment", response.getFilename());
        headers.setContentLength(response.getFileSize());

        return ResponseEntity.ok()
                .headers(headers)
                .body(response.getContent());
    }
}
