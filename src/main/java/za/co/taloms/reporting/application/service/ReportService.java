package za.co.taloms.reporting.application.service;

import za.co.taloms.reporting.application.dto.ReportRequest;
import za.co.taloms.reporting.application.dto.ReportResponse;

public interface ReportService {
    ReportResponse generatePtoOccupancyRegisterReport(ReportRequest request);
    ReportResponse generateLandParcelUtilisationReport(ReportRequest request);
    ReportResponse generateStandAllocationReport(ReportRequest request);
    ReportResponse generateVillagePopulationReport(ReportRequest request);
    ReportResponse generateHouseholdRegisterReport(ReportRequest request);
    ReportResponse generateResidentDemographicsReport(ReportRequest request);
    ReportResponse generateBusinessOccupancyReport(ReportRequest request);
    ReportResponse generateEconomicActivityReport(ReportRequest request);
    ReportResponse generateUserActivityAuditReport(ReportRequest request);
    ReportResponse generateDocumentManagementReport(ReportRequest request);
    ReportResponse generatePerformanceDashboardReport(ReportRequest request);
    ReportResponse generateLandBoundaryReport(ReportRequest request);
}