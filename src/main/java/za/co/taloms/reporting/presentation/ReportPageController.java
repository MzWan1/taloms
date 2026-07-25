package za.co.taloms.reporting.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import za.co.taloms.audit.application.service.AuditService;
import za.co.taloms.businessoccupancy.application.service.BusinessOccupancyService;
import za.co.taloms.document.application.service.DocumentService;
import za.co.taloms.household.application.service.HouseholdService;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.reporting.domain.entity.ReportFormat;
import za.co.taloms.reporting.domain.entity.ReportType;
import za.co.taloms.resident.application.service.ResidentService;
import za.co.taloms.security.application.service.UserService;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;
import java.time.LocalDate;

@Slf4j
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportPageController {

    private final PTOService ptoService;
    private final ParcelService parcelService;
    private final ResidentService residentService;
    private final HouseholdService householdService;
    private final BusinessOccupancyService businessOccupancyService;
    private final AuditService auditService;
    private final DocumentService documentService;
    private final UserService userService;
    private final TraditionalAuthorityService authorityService;
    private final VillageService villageService;

    @GetMapping
    public String index(Model model,
                        @RequestParam(required = false) Long authorityId,
                        @RequestParam(required = false) Long villageId,
                        @RequestParam(required = false) String dateFrom,
                        @RequestParam(required = false) String dateTo) {
        try {
            var authorities = authorityService.findAllActive();
            var villages = villageService.findAll();

            model.addAttribute("authorities", authorities);
            model.addAttribute("villages", villages);
            model.addAttribute("reportTypes", ReportType.values());
            model.addAttribute("reportFormats", ReportFormat.values());
            model.addAttribute("authorityId", authorityId);
            model.addAttribute("villageId", villageId);
            model.addAttribute("dateFrom", dateFrom);
            model.addAttribute("dateTo", dateTo);

            boolean hasFilter = authorityId != null || villageId != null;

            if (hasFilter) {
                model.addAttribute("totalPTOs", ptoService.countAll());
                model.addAttribute("pendingPTOs", ptoService.countByStatus(za.co.taloms.pto.domain.entity.PTOStatus.PENDING));

                if (villageId != null) {
                    model.addAttribute("totalParcels", parcelService.countByVillage(villageId));
                    model.addAttribute("totalResidents", residentService.countByVillageId(villageId));
                    model.addAttribute("totalHouseholds", householdService.countByVillageId(villageId));
                    model.addAttribute("totalBusinesses", businessOccupancyService.countByVillageId(villageId));
                    model.addAttribute("activePTOs", ptoService.countByVillageIdAndStatus(villageId, za.co.taloms.pto.domain.entity.PTOStatus.ACTIVE));
                    model.addAttribute("availableParcels", parcelService.countByStatusAndVillage(za.co.taloms.parcel.domain.entity.ParcelStatus.AVAILABLE, villageId));
                    model.addAttribute("allocatedParcels", parcelService.countByStatusAndVillage(za.co.taloms.parcel.domain.entity.ParcelStatus.ALLOCATED, villageId));
                    model.addAttribute("disputedParcels", parcelService.countByStatusAndVillage(za.co.taloms.parcel.domain.entity.ParcelStatus.DISPUTED, villageId));
                    model.addAttribute("parcelReserved", parcelService.countByStatusAndVillage(za.co.taloms.parcel.domain.entity.ParcelStatus.RESERVED, villageId));
                    model.addAttribute("parcelInactive", parcelService.countByStatusAndVillage(za.co.taloms.parcel.domain.entity.ParcelStatus.INACTIVE, villageId));
                    model.addAttribute("activeResidents", residentService.countByVillageId(villageId));
                    model.addAttribute("activeHouseholds", householdService.countByVillageId(villageId));
                    model.addAttribute("activeBusinesses", businessOccupancyService.countByVillageId(villageId));
                    model.addAttribute("ptoPending", ptoService.countByVillageIdAndStatus(villageId, za.co.taloms.pto.domain.entity.PTOStatus.PENDING));
                    model.addAttribute("ptoActive", ptoService.countByVillageIdAndStatus(villageId, za.co.taloms.pto.domain.entity.PTOStatus.ACTIVE));
                    model.addAttribute("ptoSuspended", ptoService.countByVillageIdAndStatus(villageId, za.co.taloms.pto.domain.entity.PTOStatus.SUSPENDED));
                    model.addAttribute("ptoRevoked", ptoService.countByVillageIdAndStatus(villageId, za.co.taloms.pto.domain.entity.PTOStatus.REVOKED));
                    model.addAttribute("ptoExpired", ptoService.countByVillageIdAndStatus(villageId, za.co.taloms.pto.domain.entity.PTOStatus.EXPIRED));
                } else if (authorityId != null) {
                    model.addAttribute("totalParcels", parcelService.countAll());
                    model.addAttribute("totalResidents", residentService.countAll());
                    model.addAttribute("totalHouseholds", householdService.countAll());
                    model.addAttribute("totalBusinesses", businessOccupancyService.countAll());
                    model.addAttribute("activePTOs", ptoService.countByTraditionalAuthorityIdAndStatus(authorityId, za.co.taloms.pto.domain.entity.PTOStatus.ACTIVE));
                    model.addAttribute("availableParcels", parcelService.countByStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.AVAILABLE));
                    model.addAttribute("allocatedParcels", parcelService.countByStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.ALLOCATED));
                    model.addAttribute("disputedParcels", parcelService.countByStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.DISPUTED));
                    model.addAttribute("parcelReserved", parcelService.countByStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.RESERVED));
                    model.addAttribute("parcelInactive", parcelService.countByStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.INACTIVE));
                    model.addAttribute("activeResidents", residentService.countActive());
                    model.addAttribute("activeHouseholds", householdService.countActive());
                    model.addAttribute("activeBusinesses", businessOccupancyService.countByStatus(za.co.taloms.businessoccupancy.domain.entity.BusinessStatus.ACTIVE));
                    model.addAttribute("ptoPending", ptoService.countByTraditionalAuthorityIdAndStatus(authorityId, za.co.taloms.pto.domain.entity.PTOStatus.PENDING));
                    model.addAttribute("ptoActive", ptoService.countByTraditionalAuthorityIdAndStatus(authorityId, za.co.taloms.pto.domain.entity.PTOStatus.ACTIVE));
                    model.addAttribute("ptoSuspended", ptoService.countByTraditionalAuthorityIdAndStatus(authorityId, za.co.taloms.pto.domain.entity.PTOStatus.SUSPENDED));
                    model.addAttribute("ptoRevoked", ptoService.countByTraditionalAuthorityIdAndStatus(authorityId, za.co.taloms.pto.domain.entity.PTOStatus.REVOKED));
                    model.addAttribute("ptoExpired", ptoService.countByTraditionalAuthorityIdAndStatus(authorityId, za.co.taloms.pto.domain.entity.PTOStatus.EXPIRED));
                }

                model.addAttribute("maleResidents", residentService.countByGender(za.co.taloms.resident.domain.entity.Gender.MALE.name()));
                model.addAttribute("femaleResidents", residentService.countByGender(za.co.taloms.resident.domain.entity.Gender.FEMALE.name()));
                model.addAttribute("unknownGenderResidents", residentService.countByGenderUnknown());
            } else {
                model.addAttribute("totalPTOs", ptoService.countAll());
                model.addAttribute("activePTOs", ptoService.countByStatus(za.co.taloms.pto.domain.entity.PTOStatus.ACTIVE));
                model.addAttribute("pendingPTOs", ptoService.countByStatus(za.co.taloms.pto.domain.entity.PTOStatus.PENDING));
                model.addAttribute("totalParcels", parcelService.countAll());
                model.addAttribute("availableParcels", parcelService.countByStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.AVAILABLE));
                model.addAttribute("allocatedParcels", parcelService.countByStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.ALLOCATED));
                model.addAttribute("disputedParcels", parcelService.countByStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.DISPUTED));
                model.addAttribute("totalResidents", residentService.countAll());
                model.addAttribute("activeResidents", residentService.countActive());
                model.addAttribute("totalHouseholds", householdService.countAll());
                model.addAttribute("activeHouseholds", householdService.countActive());
                model.addAttribute("totalBusinesses", businessOccupancyService.countAll());
                model.addAttribute("activeBusinesses", businessOccupancyService.countByStatus(za.co.taloms.businessoccupancy.domain.entity.BusinessStatus.ACTIVE));
                model.addAttribute("ptoPending", ptoService.countByStatus(za.co.taloms.pto.domain.entity.PTOStatus.PENDING));
                model.addAttribute("ptoActive", ptoService.countByStatus(za.co.taloms.pto.domain.entity.PTOStatus.ACTIVE));
                model.addAttribute("ptoSuspended", ptoService.countByStatus(za.co.taloms.pto.domain.entity.PTOStatus.SUSPENDED));
                model.addAttribute("ptoRevoked", ptoService.countByStatus(za.co.taloms.pto.domain.entity.PTOStatus.REVOKED));
                model.addAttribute("ptoExpired", ptoService.countByStatus(za.co.taloms.pto.domain.entity.PTOStatus.EXPIRED));
                model.addAttribute("parcelAvailable", parcelService.countByStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.AVAILABLE));
                model.addAttribute("parcelAllocated", parcelService.countByStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.ALLOCATED));
                model.addAttribute("parcelDisputed", parcelService.countByStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.DISPUTED));
                model.addAttribute("parcelReserved", parcelService.countByStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.RESERVED));
                model.addAttribute("parcelInactive", parcelService.countByStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.INACTIVE));
                model.addAttribute("maleResidents", residentService.countByGender(za.co.taloms.resident.domain.entity.Gender.MALE.name()));
                model.addAttribute("femaleResidents", residentService.countByGender(za.co.taloms.resident.domain.entity.Gender.FEMALE.name()));
                model.addAttribute("unknownGenderResidents", residentService.countByGenderUnknown());
            }

            model.addAttribute("totalDocuments", documentService.countAll());
            model.addAttribute("totalUsers", userService.countAll());
            model.addAttribute("activeUsers", userService.countActive());
            model.addAttribute("totalAuditLogs", auditService.countAll());

            model.addAttribute("pageTitle", "Reports");
            model.addAttribute("currentPage", "reports");
            model.addAttribute("hasFilter", hasFilter);
            return "reports/index";
        } catch (Exception e) {
            log.error("Error loading reports page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading reports: " + e.getMessage());
            model.addAttribute("pageTitle", "Reports");
            model.addAttribute("currentPage", "reports");
            return "reports/index";
        }
    }
}