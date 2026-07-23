package za.co.taloms.reporting.application.dto;

import za.co.taloms.parcel.application.dto.ParcelResponse;
import za.co.taloms.pto.application.dto.PTOResponse;
import za.co.taloms.businessoccupancy.application.dto.BusinessOccupancyResponse;
import za.co.taloms.household.application.dto.HouseholdResponse;
import za.co.taloms.resident.application.dto.ResidentResponse;
import za.co.taloms.audit.application.dto.AuditLogResponse;
import za.co.taloms.document.application.dto.DocumentResponse;

import java.time.LocalDate;
import java.util.List;

public record ReportData(
    String reportTitle,
    String authorityName,
    String villageName,
    LocalDate dateFrom,
    LocalDate dateTo,
    List<ReportSection> sections,
    List<PTOResponse> ptos,
    List<ParcelResponse> parcels,
    List<ResidentResponse> residents,
    List<HouseholdResponse> households,
    List<BusinessOccupancyResponse> businesses,
    List<AuditLogResponse> auditLogs,
    List<DocumentResponse> documents
) {}
