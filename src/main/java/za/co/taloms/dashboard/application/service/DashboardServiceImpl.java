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
            // PTO Counts
            long totalPtos = ptoService.countAll();
            long activePtos = ptoService.countByStatus(PTOStatus.ACTIVE);
            long pendingPtos = ptoService.countByStatus(PTOStatus.PENDING);
            long suspendedPtos = ptoService.countByStatus(PTOStatus.SUSPENDED);
            long revokedPtos = ptoService.countByStatus(PTOStatus.REVOKED);

            // Parcel Counts
            long totalParcels = parcelService.countAll();
            long availableParcels = parcelService.countByStatus(ParcelStatus.AVAILABLE);
            long allocatedParcels = parcelService.countByStatus(ParcelStatus.ALLOCATED);
            long disputedParcels = parcelService.countByStatus(ParcelStatus.DISPUTED);
            long reservedParcels = parcelService.countByStatus(ParcelStatus.RESERVED);

            // Household Counts
            long totalHouseholds = householdService.countAll();
            long activeHouseholds = householdService.countActive();

            // Resident Counts
            long totalResidents = residentService.countAll();
            long activeResidents = residentService.countActive();

            // Business Counts
            long totalBusinesses = businessService.countAll();
            long activeBusinesses = businessService.countByStatus(BusinessStatus.ACTIVE);
            long pendingBusinesses = businessService.countByStatus(BusinessStatus.PENDING);

            // Document Counts
            long totalDocuments = documentService.countAll();

            // User Counts (Admin only)
            long totalUsers = userService.countAll();
            long activeUsers = userService.countActive();

            // Audit Counts
            long totalAuditLogs = auditService.countAll();

            // Recent Activity
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
        // Future enhancement: filter counts by authority
        return getDashboardSummary();
    }

    private List<RecentActivityDto> getRecentActivity() {
        try {
            var auditLogs = auditService.findAll();
            return auditLogs.stream()
                    .limit(10)
                    .map(audit -> RecentActivityDto.builder()
                            .action(audit.getActionDisplay())
                            .entityType(audit.getEntityType())
                            .entityTypeDisplay(audit.getEntityTypeDisplay())
                            .entityId(audit.getEntityId())
                            .performedBy(audit.getPerformedBy())
                            .performedAt(audit.getPerformedAt())
                            .performedAtDisplay(audit.getPerformedAtDisplay())
                            .description(audit.getDescription())
                            .build())
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting recent activity: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}