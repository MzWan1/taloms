package za.co.taloms.reporting.application.service;

import za.co.taloms.reporting.application.dto.ReportRequest;
import za.co.taloms.reporting.application.dto.ReportResponse;

public interface ReportService {
    ReportResponse generatePTOReport(ReportRequest request);
    ReportResponse generateParcelReport(ReportRequest request);
    ReportResponse generatePopulationReport(ReportRequest request);
    ReportResponse generateLandUtilisationReport(ReportRequest request);
}