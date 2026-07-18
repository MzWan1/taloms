package za.co.taloms.dashboard.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.audit.application.service.AuditService;
import za.co.taloms.businessoccupancy.application.service.BusinessOccupancyService;
import za.co.taloms.businessoccupancy.domain.entity.BusinessStatus;
import za.co.taloms.dashboard.application.dto.DashboardSummaryDto;
import za.co.taloms.dashboard.application.dto.RecentActivityDto;
import za.co.taloms.household.application.service.HouseholdService;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.resident.application.service.ResidentService;
import za.co.taloms.security.application.service.UserService;
import za.co.taloms.document.application.service.DocumentService;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final PTOService ptoService;
    private final ParcelService parcelService;
    private final HouseholdService householdService;
    private final ResidentService residentService;
    private final BusinessOccupancyService businessService;
    private final DocumentService documentService;
    private final UserService userService;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        try {
            // PTO Counts - handle null safely
            Long totalPtos = safeLong(() -> ptoService.countAll());
            Long activePtos = safeLong(() -> ptoService.countByStatus(PTOStatus.ACTIVE));
            Long pendingPtos = safeLong(() -> ptoService.countByStatus(PTOStatus.PENDING));
            Long suspendedPtos = safeLong(() -> ptoService.countByStatus(PTOStatus.SUSPENDED));
            Long revokedPtos = safeLong(() -> ptoService.countByStatus(PTOStatus.REVOKED));

            // Parcel Counts
            Long totalParcels = safeLong(() -> parcelService.countAll());
            Long availableParcels = safeLong(() -> parcelService.countByStatus(ParcelStatus.AVAILABLE));
            Long allocatedParcels = safeLong(() -> parcelService.countByStatus(ParcelStatus.ALLOCATED));
            Long disputedParcels = safeLong(() -> parcelService.countByStatus(ParcelStatus.DISPUTED));
            Long reservedParcels = safeLong(() -> parcelService.countByStatus(ParcelStatus.RESERVED));

            // Household Counts
            Long totalHouseholds = safeLong(() -> householdService.countAll());
            Long activeHouseholds = safeLong(() -> householdService.countActive());

            // Resident Counts
            Long totalResidents = safeLong(() -> residentService.countAll());
            Long activeResidents = safeLong(() -> residentService.countActive());

            // Business Counts
            Long totalBusinesses = safeLong(() -> businessService.countAll());
            Long activeBusinesses = safeLong(() -> businessService.countByStatus(BusinessStatus.ACTIVE));
            Long pendingBusinesses = safeLong(() -> businessService.countByStatus(BusinessStatus.PENDING));

            // Document Counts
            Long totalDocuments = safeLong(() -> documentService.countAll());

            // User Counts (Admin only)
            Long totalUsers = safeLong(() -> userService.countAll());
            Long activeUsers = safeLong(() -> userService.countActive());

            // Audit Counts
            Long totalAuditLogs = safeLong(() -> auditService.countAll());

            // Recent Activity - handle gracefully if audit fails
            List<RecentActivityDto> recentActivity = getRecentActivity();

            return DashboardSummaryDto.builder()
                    .totalPtos(totalPtos)
                    .activePtos(activePtos)
                    .pendingPtos(pendingPtos)
                    .suspendedPtos(suspendedPtos)
                    .revokedPtos(revokedPtos)
                    .totalParcels(totalParcels)
                    .availableParcels(availableParcels)
                    .allocatedParcels(allocatedParcels)
                    .disputedParcels(disputedParcels)
                    .reservedParcels(reservedParcels)
                    .totalHouseholds(totalHouseholds)
                    .activeHouseholds(activeHouseholds)
                    .totalResidents(totalResidents)
                    .activeResidents(activeResidents)
                    .totalBusinesses(totalBusinesses)
                    .activeBusinesses(activeBusinesses)
                    .pendingBusinesses(pendingBusinesses)
                    .totalDocuments(totalDocuments)
                    .totalUsers(totalUsers)
                    .activeUsers(activeUsers)
                    .totalAuditLogs(totalAuditLogs)
                    .recentActivity(recentActivity)
                    .build();

        } catch (Exception e) {
            log.error("Error building dashboard summary: {}", e.getMessage(), e);
            // Return empty dashboard with zeros
            return DashboardSummaryDto.builder()
                    .totalPtos(0L)
                    .activePtos(0L)
                    .pendingPtos(0L)
                    .suspendedPtos(0L)
                    .revokedPtos(0L)
                    .totalParcels(0L)
                    .availableParcels(0L)
                    .allocatedParcels(0L)
                    .disputedParcels(0L)
                    .reservedParcels(0L)
                    .totalHouseholds(0L)
                    .activeHouseholds(0L)
                    .totalResidents(0L)
                    .activeResidents(0L)
                    .totalBusinesses(0L)
                    .activeBusinesses(0L)
                    .pendingBusinesses(0L)
                    .totalDocuments(0L)
                    .totalUsers(0L)
                    .activeUsers(0L)
                    .totalAuditLogs(0L)
                    .recentActivity(new ArrayList<>())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummaryForAuthority(Long authorityId) {
        // This would filter by authority - for now, return global summary
        return getDashboardSummary();
    }

    private List<RecentActivityDto> getRecentActivity() {
        try {
            var auditLogs = auditService.findAll();
            if (auditLogs == null || auditLogs.isEmpty()) {
                return new ArrayList<>();
            }
            return auditLogs.stream()
                    .limit(10)
                    .map(audit -> {
                        try {
                            return RecentActivityDto.builder()
                                    .action(audit.getActionDisplay() != null ? audit.getActionDisplay() : "Unknown")
                                    .actionDisplay(audit.getActionDisplay() != null ? audit.getActionDisplay() : "Unknown")
                                    .badgeClass(audit.getActionBadgeClass() != null ? audit.getActionBadgeClass() : "bg-secondary")
                                    .entityType(audit.getEntityType() != null ? audit.getEntityType() : "Unknown")
                                    .entityTypeDisplay(audit.getEntityTypeDisplay() != null ? audit.getEntityTypeDisplay() : "Unknown")
                                    .entityId(audit.getEntityId() != null ? audit.getEntityId() : 0L)
                                    .performedBy(audit.getPerformedBy() != null ? audit.getPerformedBy() : "System")
                                    .performedAt(audit.getPerformedAt())
                                    .performedAtDisplay(audit.getPerformedAtDisplay() != null ? audit.getPerformedAtDisplay() : "")
                                    .description(audit.getDescription() != null ? audit.getDescription() : "")
                                    .build();
                        } catch (Exception e) {
                            log.warn("Error converting audit log to recent activity: {}", e.getMessage());
                            return RecentActivityDto.builder()
                                    .action("Unknown")
                                    .actionDisplay("Unknown")
                                    .badgeClass("bg-secondary")
                                    .entityType("Unknown")
                                    .entityTypeDisplay("Unknown")
                                    .entityId(0L)
                                    .performedBy("System")
                                    .description("Error loading activity")
                                    .build();
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting recent activity: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // Helper method to safely get long values
    private Long safeLong(java.util.function.Supplier<Long> supplier) {
        try {
            Long value = supplier.get();
            return value != null ? value : 0L;
        } catch (Exception e) {
            log.warn("Error getting count: {}", e.getMessage());
            return 0L;
        }
    }
}