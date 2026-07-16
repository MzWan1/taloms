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
        log.info("Generating PTO Report - Format: {}", request.getFormat());
        ReportService exporter = getExporter(request.getFormat());
        ReportResponse response = exporter.generatePTOReport(request);
        return buildResponse(response);
    }

    @PostMapping("/parcel")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<byte[]> generateParcelReport(@RequestBody ReportRequest request) {
        log.info("Generating Parcel Report - Format: {}", request.getFormat());
        ReportService exporter = getExporter(request.getFormat());
        ReportResponse response = exporter.generateParcelReport(request);
        return buildResponse(response);
    }

    @PostMapping("/population")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<byte[]> generatePopulationReport(@RequestBody ReportRequest request) {
        log.info("Generating Population Report - Format: {}", request.getFormat());
        ReportService exporter = getExporter(request.getFormat());
        ReportResponse response = exporter.generatePopulationReport(request);
        return buildResponse(response);
    }

    @PostMapping("/land-utilisation")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<byte[]> generateLandUtilisationReport(@RequestBody ReportRequest request) {
        log.info("Generating Land Utilisation Report - Format: {}", request.getFormat());
        ReportService exporter = getExporter(request.getFormat());
        ReportResponse response = exporter.generateLandUtilisationReport(request);
        return buildResponse(response);
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