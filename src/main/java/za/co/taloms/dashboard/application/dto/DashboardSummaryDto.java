package za.co.taloms.dashboard.application.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    // PTO KPIs
    private Long totalPtos;
    private Long activePtos;
    private Long pendingPtos;
    private Long suspendedPtos;
    private Long revokedPtos;

    // Parcel KPIs
    private Long totalParcels;
    private Long availableParcels;
    private Long allocatedParcels;
    private Long disputedParcels;
    private Long reservedParcels;

    // Household KPIs
    private Long totalHouseholds;
    private Long activeHouseholds;

    // Resident KPIs
    private Long totalResidents;
    private Long activeResidents;

    // Business KPIs
    private Long totalBusinesses;
    private Long activeBusinesses;
    private Long pendingBusinesses;

    // Document KPIs
    private Long totalDocuments;

    // User KPIs (Admin only)
    private Long totalUsers;
    private Long activeUsers;

    // Audit KPIs
    private Long totalAuditLogs;

    // Recent Activity
    private List<RecentActivityDto> recentActivity;
}