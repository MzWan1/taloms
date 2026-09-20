package za.co.taloms.dashboard.application.dto;

import lombok.*;
import za.co.taloms.common.MaskedLongSerializer;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    // PTO KPIs - these are counts, not masked
    private Long totalPtos;
    private Long activePtos;
    private Long pendingPtos;
    private Long suspendedPtos;
    private Long revokedPtos;

    // Parcel KPIs - these are counts, not masked
    private Long totalParcels;
    private Long availableParcels;
    private Long allocatedParcels;
    private Long disputedParcels;
    private Long reservedParcels;

    // Document KPIs - this is a count, not masked
    private Long totalDocuments;

    // Additional KPIs
    private Long totalVillages;
    private Long totalCompanies;

    // User KPIs (Admin only) - these are counts, not masked
    private Long totalUsers;
    private Long activeUsers;

    // Audit KPIs - this is a count, not masked
    private Long totalAuditLogs;

    // Approval Queue
    private List<PendingPtoSummaryDto> pendingPtoSummaries;

    // Recent Activity
    private List<RecentActivityDto> recentActivity;
}

