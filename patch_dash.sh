#!/bin/bash
cat << 'INNER_EOF' > src/main/java/za/co/taloms/dashboard/application/service/DashboardServiceImpl.java
package za.co.taloms.dashboard.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.audit.application.service.AuditService;
import za.co.taloms.company.application.service.CompanyService;
import za.co.taloms.dashboard.application.dto.DashboardSummaryDto;
import za.co.taloms.dashboard.application.dto.PendingPtoSummaryDto;
import za.co.taloms.dashboard.application.dto.RecentActivityDto;
import za.co.taloms.document.application.service.DocumentService;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.security.application.service.AuthorityScopeService;
import za.co.taloms.security.application.service.UserService;
import za.co.taloms.traditionalauthority.application.service.VillageService;
import za.co.taloms.pto.application.dto.PTOResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final PTOService ptoService;
    private final ParcelService parcelService;
    private final DocumentService documentService;
    private final UserService userService;
    private final AuditService auditService;
    private final AuthorityScopeService authorityScopeService;
    private final VillageService villageService;
    private final CompanyService companyService;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        try {
            boolean isAdmin = authorityScopeService.isCurrentUserAdmin();
            boolean isChiefOrHeadsman = authorityScopeService.isCurrentUserChiefOrHeadsman();

            Long totalPtos = 0L, activePtos = 0L, pendingPtos = 0L, suspendedPtos = 0L, revokedPtos = 0L;
            Long totalParcels = 0L, availableParcels = 0L, allocatedParcels = 0L, disputedParcels = 0L, reservedParcels = 0L;
            Long totalDocuments = 0L, totalUsers = 0L, activeUsers = 0L, totalAuditLogs = 0L, totalVillages = 0L, totalCompanies = 0L;
            List<RecentActivityDto> recentActivity = new ArrayList<>();
            List<PendingPtoSummaryDto> pendingPtoSummaries = new ArrayList<>();

            if (isAdmin) {
                totalPtos = safeLong(() -> ptoService.countAll());
                activePtos = safeLong(() -> ptoService.countByStatus(PTOStatus.ACTIVE));
                pendingPtos = safeLong(() -> ptoService.countByStatus(PTOStatus.PENDING));
                suspendedPtos = safeLong(() -> ptoService.countByStatus(PTOStatus.SUSPENDED));
                revokedPtos = safeLong(() -> ptoService.countByStatus(PTOStatus.REVOKED));

                totalParcels = safeLong(() -> parcelService.countAll());
                availableParcels = safeLong(() -> parcelService.countByStatus(ParcelStatus.AVAILABLE));
                allocatedParcels = safeLong(() -> parcelService.countByStatus(ParcelStatus.ALLOCATED));
                disputedParcels = safeLong(() -> parcelService.countByStatus(ParcelStatus.DISPUTED));
                reservedParcels = safeLong(() -> parcelService.countByStatus(ParcelStatus.RESERVED));

                totalDocuments = safeLong(() -> documentService.countAll());
                totalUsers = safeLong(() -> userService.countAll());
                activeUsers = safeLong(() -> userService.countActive());
                totalAuditLogs = safeLong(() -> auditService.countAll());
                totalVillages = safeLong(() -> (long) villageService.findAll().size());
                totalCompanies = safeLong(() -> (long) companyService.findAll().size());

                recentActivity = getRecentActivity();
                pendingPtoSummaries = buildPendingPtoSummaries(null);
            } else if (isChiefOrHeadsman) {
                Set<Long> scopedVillages = authorityScopeService.scopedVillageIds();
                Set<Long> scopedAuthorities = authorityScopeService.getCurrentUserAuthorityIds();
                if (scopedVillages != null) {
                    for (Long vid : scopedVillages) {
                        totalPtos += safeLong(() -> ptoService.countByVillageId(vid));
                        activePtos += safeLong(() -> ptoService.countByVillageIdAndStatus(vid, PTOStatus.ACTIVE));
                        pendingPtos += safeLong(() -> ptoService.countByVillageIdAndStatus(vid, PTOStatus.PENDING));
                        suspendedPtos += safeLong(() -> ptoService.countByVillageIdAndStatus(vid, PTOStatus.SUSPENDED));
                        revokedPtos += safeLong(() -> ptoService.countByVillageIdAndStatus(vid, PTOStatus.REVOKED));

                        totalParcels += safeLong(() -> parcelService.countByVillage(vid));
                        availableParcels += safeLong(() -> parcelService.countByStatusAndVillage(ParcelStatus.AVAILABLE, vid));
                        allocatedParcels += safeLong(() -> parcelService.countByStatusAndVillage(ParcelStatus.ALLOCATED, vid));
                        disputedParcels += safeLong(() -> parcelService.countByStatusAndVillage(ParcelStatus.DISPUTED, vid));
                        reservedParcels += safeLong(() -> parcelService.countByStatusAndVillage(ParcelStatus.RESERVED, vid));
                    }
                    totalVillages = (long) scopedVillages.size();
                }
                
                pendingPtoSummaries = buildPendingPtoSummaries(scopedVillages);
                recentActivity = new ArrayList<>();
            }

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
                    .totalDocuments(totalDocuments)
                    .totalUsers(totalUsers)
                    .activeUsers(activeUsers)
                    .totalAuditLogs(totalAuditLogs)
                    .totalVillages(totalVillages)
                    .totalCompanies(totalCompanies)
                    .pendingPtoSummaries(pendingPtoSummaries)
                    .recentActivity(recentActivity)
                    .build();

        } catch (Exception e) {
            log.error("Error building dashboard summary: {}", e.getMessage(), e);
            return DashboardSummaryDto.builder()
                    .totalPtos(0L).activePtos(0L).pendingPtos(0L).suspendedPtos(0L).revokedPtos(0L)
                    .totalParcels(0L).availableParcels(0L).allocatedParcels(0L).disputedParcels(0L).reservedParcels(0L)
                    .totalDocuments(0L).totalUsers(0L).activeUsers(0L).totalAuditLogs(0L)
                    .totalVillages(0L).totalCompanies(0L)
                    .pendingPtoSummaries(new ArrayList<>())
                    .recentActivity(new ArrayList<>())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummaryForAuthority(Long authorityId) {
        return getDashboardSummary();
    }

    private List<RecentActivityDto> getRecentActivity() {
        try {
            var auditLogs = auditService.findRecent(5);
            if (auditLogs == null || auditLogs.isEmpty()) return new ArrayList<>();
            return auditLogs.stream().map(audit -> RecentActivityDto.builder()
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
                    .build()
            ).collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Long safeLong(java.util.function.Supplier<Long> supplier) {
        try {
            Long value = supplier.get();
            return value != null ? value : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private List<PendingPtoSummaryDto> buildPendingPtoSummaries(Set<Long> scopedVillages) {
        try {
            List<PTOResponse> ptos;
            if (scopedVillages == null) {
                ptos = ptoService.findTop5ByStatus(PTOStatus.PENDING);
            } else {
                ptos = ptoService.findTop5ByVillagesAndStatus(scopedVillages, PTOStatus.PENDING);
            }
            return ptos.stream()
                    .map(pto -> new PendingPtoSummaryDto(
                            pto.getId(),
                            pto.getPtoNumber() != null ? pto.getPtoNumber() : "",
                            pto.getPtoHolderName() != null ? pto.getPtoHolderName() : "",
                            pto.getIdNumber() != null ? pto.getIdNumber() : "",
                            pto.getVillageName() != null ? pto.getVillageName() : "",
                            pto.getAuthorityName() != null ? pto.getAuthorityName() : "",
                            pto.getIssueDate()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
INNER_EOF
