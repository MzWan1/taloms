package za.co.taloms.reporting.application.service;

import za.co.taloms.reporting.application.dto.ReportData;
import za.co.taloms.reporting.application.dto.ReportRequest;
import za.co.taloms.traditionalauthority.application.dto.VillageResponse;
import za.co.taloms.traditionalauthority.application.dto.TraditionalAuthorityResponse;
import java.time.LocalDate;
import java.util.List;

public interface ReportDataService {

    ReportData generatePtoOccupancyRegisterReport(ReportRequest request);

    ReportData generateLandParcelUtilisationReport(ReportRequest request);

    ReportData generateStandAllocationReport(ReportRequest request);

    ReportData generateVillagePopulationReport(ReportRequest request);

    ReportData generateHouseholdRegisterReport(ReportRequest request);

    ReportData generateResidentDemographicsReport(ReportRequest request);

    ReportData generateBusinessOccupancyReport(ReportRequest request);

    ReportData generateEconomicActivityReport(ReportRequest request);

    ReportData generateUserActivityAuditReport(ReportRequest request);

    ReportData generateDocumentManagementReport(ReportRequest request);

    ReportData generatePerformanceDashboardReport(ReportRequest request);

    ReportData generateLandBoundaryReport(ReportRequest request);

    TraditionalAuthorityResponse getAuthority(Long authorityId);

    VillageResponse getVillage(Long villageId);
}
