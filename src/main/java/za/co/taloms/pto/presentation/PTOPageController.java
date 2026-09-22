package za.co.taloms.pto.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.common.IdMasker;
import za.co.taloms.document.application.dto.DocumentResponse;
import za.co.taloms.document.application.service.DocumentService;
import za.co.taloms.document.domain.entity.EntityType;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.pto.application.dto.*;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.pto.domain.entity.PTOPurpose;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.security.application.service.AuthorityScopeService;
import za.co.taloms.traditionalauthority.application.dto.TraditionalAuthorityResponse;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.co.taloms.common.pagination.PageRequestUtils;

@Slf4j
@Controller
@RequestMapping("/ptos")
@RequiredArgsConstructor
public class PTOPageController {

    private final PTOService ptoService;
    private final ParcelService parcelService;
    private final TraditionalAuthorityService authorityService;
    private final VillageService villageService;
    private final DocumentService documentService;
    private final AuthorityScopeService scopeService;

    /** Throws SecurityException if the PTO is outside the current user's scope. */
    private void requirePtoAccess(Long ptoId) {
        var pto = ptoService.findById(ptoId);
        if (pto == null || pto.getVillageId() == null) {
            throw new SecurityException("PTO not found or has no linked village.");
        }
        scopeService.requireVillageAccess(pto.getVillageId());
    }

    @GetMapping
    public String list(Model model,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false, defaultValue = "1") Integer page) {
        try {
            var criteria = PTOSearchCriteria.builder();
            if (status != null && !status.isBlank()) {
                criteria.status(PTOStatus.valueOf(status));
            }
            if (search != null && !search.isBlank()) {
                criteria.holderName(search);
            }

            // Chiefs/headsmen are scoped to the villages they manage
            boolean scopedUser = scopeService.isCurrentUserChiefOrHeadsman();
            Set<Long> scopedVillageIds = scopedUser ? scopeService.scopedVillageIds() : null;


            Pageable pageable = PageRequestUtils.toPageable(page, 10);
            Page<PTOResponse> pageObj;

            if (scopedUser && (scopedVillageIds == null || scopedVillageIds.isEmpty())) {
                // Chief/headman scoped to no village sees nothing
                pageObj = new org.springframework.data.domain.PageImpl<>(Collections.emptyList(), pageable, 0);
            } else if ((status != null && !status.isBlank())
                    || (search != null && !search.isBlank())
                    || scopedVillageIds != null) {
                if (scopedVillageIds != null) {
                    criteria.villageIds(scopedVillageIds);
                }
                pageObj = ptoService.search(criteria.build(), pageable);
            } else {
                pageObj = ptoService.findAll(pageable);
            }

            model.addAttribute("page", pageObj);
            model.addAttribute("ptos", pageObj.getContent());
            model.addAttribute("statuses", PTOStatus.values());
            model.addAttribute("purposes", PTOPurpose.values());
            model.addAttribute("totalCount", pageObj.getTotalElements());
            // pendingCount cannot be easily computed from just the page, so we use count queries
            model.addAttribute("pendingCount", scopedUser ? 
                (scopedVillageIds != null && !scopedVillageIds.isEmpty() ? 
                    scopedVillageIds.stream().mapToLong(vid -> ptoService.countByVillageIdAndStatus(vid, PTOStatus.PENDING)).sum() : 0) 
                : ptoService.countByStatus(PTOStatus.PENDING));
            model.addAttribute("activeCount", scopedUser ? 
                (scopedVillageIds != null && !scopedVillageIds.isEmpty() ? 
                    scopedVillageIds.stream().mapToLong(vid -> ptoService.countByVillageIdAndStatus(vid, PTOStatus.ACTIVE)).sum() : 0) 
                : ptoService.countByStatus(PTOStatus.ACTIVE));
            model.addAttribute("revokedCount", scopedUser ? 
                (scopedVillageIds != null && !scopedVillageIds.isEmpty() ? 
                    scopedVillageIds.stream().mapToLong(vid -> ptoService.countByVillageIdAndStatus(vid, PTOStatus.REVOKED)).sum() : 0) 
                : ptoService.countByStatus(PTOStatus.REVOKED));

            model.addAttribute("selectedStatus", status);
            model.addAttribute("searchTerm", search);
            model.addAttribute("pageTitle", "PTO Management");
            model.addAttribute("currentPage", "ptos");
            return "ptos/list";
        } catch (Exception e) {
            log.error("Error loading PTO list: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading PTOs: " + e.getMessage());
            model.addAttribute("ptos", Collections.emptyList());
            model.addAttribute("statuses", PTOStatus.values());
            model.addAttribute("purposes", PTOPurpose.values());
            model.addAttribute("totalCount", 0L);
            model.addAttribute("pendingCount", 0L);
            model.addAttribute("activeCount", 0L);
            model.addAttribute("revokedCount", 0L);
            model.addAttribute("pageTitle", "PTO Management");
            model.addAttribute("currentPage", "ptos");
            return "ptos/list";
        }
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        try {
            // Chiefs/headsmen can only create PTOs for the villages they manage
            boolean scopedUser = scopeService.isCurrentUserChiefOrHeadsman();
            Set<Long> scopedVillageIds = scopedUser ? scopeService.scopedVillageIds() : null;

            // Authorities are needed for the PTO form's authority field.
            // For chiefs/headsmen, only authorities they belong to are shown.
            java.util.Set<Long> allowedAuthorityIds =
                    scopedUser ? scopeService.getCurrentUserAuthorityIds() : null;
            var authorities = scopedUser
                    ? (allowedAuthorityIds == null || allowedAuthorityIds.isEmpty()
                        ? Collections.emptyList()
                        : authorityService.findAllActive().stream()
                            .filter(a -> allowedAuthorityIds.contains(a.getId()))
                            .toList())
                    : authorityService.findAllActive();
            log.info("Loaded {} active authorities for PTO create form", authorities.size());

            if (!model.containsAttribute("form")) {
                model.addAttribute("form", PTORequest.builder()
                        .issueDate(LocalDate.now())
                        .build());
            }

            // Get available parcels for PTO (scoped to the user's villages)
            Set<Long> allowedVillageIds = scopedUser ? scopedVillageIds : null;
            Set<Long> finalAllowedVillageIds = allowedVillageIds;

            List<za.co.taloms.parcel.application.dto.ParcelResponse> availableParcels = Collections.emptyList();
            try {
                var allParcels = parcelService.findAll();
                if (allParcels != null && !allParcels.isEmpty()) {
                    availableParcels = allParcels.stream()
                            .filter(p -> p != null && p.getStatus() == ParcelStatus.AVAILABLE)
                            .filter(p -> finalAllowedVillageIds == null
                                    || (p.getVillageId() != null
                                        && finalAllowedVillageIds.contains(p.getVillageId())))
                            .collect(Collectors.toList());
                    log.info("Found {} available parcels for PTO creation", availableParcels.size());
                }
            } catch (Exception e) {
                log.error("Error loading available parcels: {}", e.getMessage(), e);
            }

            model.addAttribute("authorities", authorities);
            model.addAttribute("availableParcels", availableParcels);
            model.addAttribute("purposes", PTOPurpose.values());
            model.addAttribute("pageTitle", "Create PTO");
            model.addAttribute("currentPage", "ptos");
            return "ptos/create";
        } catch (Exception e) {
            log.error("Error loading create PTO form: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading form: " + e.getMessage());
            model.addAttribute("authorities", Collections.emptyList());
            model.addAttribute("availableParcels", Collections.emptyList());
            model.addAttribute("purposes", PTOPurpose.values());
            model.addAttribute("pageTitle", "Create PTO");
            model.addAttribute("currentPage", "ptos");
            return "ptos/create";
        }
    }

    @PostMapping("/create")
    public String create(
            @RequestParam Long parcelId,
            @RequestParam String ptoHolderName,
            @RequestParam String idNumber,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) String contactEmail,
            @RequestParam String purpose,
            @RequestParam String issueDate,
            @RequestParam(required = false) String expiryDate,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String allocatedBy,
            @RequestParam(required = false) String allocationDate,
            @RequestParam(required = false) Double standArea,
            @RequestParam(required = false) String surveyReference,
            @RequestParam(required = false) String boundaryDescription,
            @RequestParam(required = false) String allocationFeeReceipt,
            @RequestParam(required = false) String taRecommendationRef,
            @RequestParam(required = false) Boolean communityResolutionRequired,
            @RequestParam(required = false) MultipartFile taAllocationLetter,
            @RequestParam(required = false) MultipartFile siteSketch,
            @RequestParam(required = false) MultipartFile idCopy,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        log.info("Creating PTO - Holder: {}, ID: {}, Parcel: {}, Purpose: {}",
                ptoHolderName, IdMasker.maskIdNumber(idNumber), parcelId, purpose);

        try {
            // Get the parcel to validate and populate stand/parcel details
            var parcel = parcelService.findById(parcelId);
            if (parcel == null) {
                ra.addFlashAttribute("errorMessage", "❌ Parcel not found. Please select a valid parcel.");
                return "redirect:/ptos/create";
            }

            // Chiefs/headsmen can only create PTOs for parcels in villages they manage
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                scopeService.requireVillageAccess(parcel.getVillageId());
            }

            var request = PTORequest.builder()
                    .parcelId(parcelId)
                    .standNumber(parcel.getStandNumber())
                    .parcelNumber(parcel.getParcelNumber())
                    .ptoHolderName(ptoHolderName)
                    .idNumber(idNumber)
                    .contactPhone(contactPhone)
                    .contactEmail(contactEmail)
                    .purpose(purpose)
                    .issueDate(LocalDate.parse(issueDate))
                    .expiryDate(expiryDate != null && !expiryDate.isBlank() ? LocalDate.parse(expiryDate) : null)
                    .notes(notes)
                    .allocatedBy(allocatedBy)
                    .allocationDate(allocationDate != null && !allocationDate.isBlank() ? LocalDate.parse(allocationDate) : null)
                    .standArea(standArea)
                    .surveyReference(surveyReference)
                    .boundaryDescription(boundaryDescription)
                    .allocationFeeReceipt(allocationFeeReceipt)
                    .taRecommendationRef(taRecommendationRef)
                    .communityResolutionRequired(communityResolutionRequired)
                    .build();

            var response = ptoService.createPTO(request, userDetails.getUsername());
            Long ptoId = response.getId();

            // Upload supporting documents
            java.util.List<String> uploadErrors = new java.util.ArrayList<>();
            if (idCopy != null && !idCopy.isEmpty()) {
                try {
                    documentService.uploadDocument(idCopy,
                            new za.co.taloms.document.application.dto.DocumentUploadRequest(
                                    za.co.taloms.document.domain.entity.DocumentType.ID_COPY.name(),
                                    za.co.taloms.document.domain.entity.EntityType.PTO.name(),
                                    ptoId,
                                    "ID / Passport Copy — " + response.getPtoHolderName(),
                                    "Uploaded during PTO creation"),
                            userDetails.getUsername(), "127.0.0.1", "Web UI");
                } catch (Exception e) {
                    String msg = "ID copy upload failed: " + e.getMessage();
                    log.warn("{} for PTO {}", msg, ptoId, e);
                    uploadErrors.add(msg);
                }
            }
            if (taAllocationLetter != null && !taAllocationLetter.isEmpty()) {
                try {
                    documentService.uploadDocument(taAllocationLetter,
                            new za.co.taloms.document.application.dto.DocumentUploadRequest(
                                    za.co.taloms.document.domain.entity.DocumentType.TA_ALLOCATION_LETTER.name(),
                                    za.co.taloms.document.domain.entity.EntityType.PTO.name(),
                                    ptoId,
                                    "Traditional Authority Allocation Letter — " + response.getPtoNumber(),
                                    "Uploaded during PTO creation"),
                            userDetails.getUsername(), "127.0.0.1", "Web UI");
                } catch (Exception e) {
                    String msg = "TA allocation letter upload failed: " + e.getMessage();
                    log.warn("{} for PTO {}", msg, ptoId, e);
                    uploadErrors.add(msg);
                }
            }
            if (siteSketch != null && !siteSketch.isEmpty()) {
                try {
                    documentService.uploadDocument(siteSketch,
                            new za.co.taloms.document.application.dto.DocumentUploadRequest(
                                    za.co.taloms.document.domain.entity.DocumentType.SITE_SKETCH.name(),
                                    za.co.taloms.document.domain.entity.EntityType.PTO.name(),
                                    ptoId,
                                    "Site Sketch — Stand " + parcel.getStandNumber(),
                                    "Uploaded during PTO creation"),
                            userDetails.getUsername(), "127.0.0.1", "Web UI");
                } catch (Exception e) {
                    String msg = "Site sketch upload failed: " + e.getMessage();
                    log.warn("{} for PTO {}", msg, ptoId, e);
                    uploadErrors.add(msg);
                }
            }

            if (!uploadErrors.isEmpty()) {
                ra.addFlashAttribute("successMessage",
                        "✅ PTO " + response.getPtoNumber() + " created, but document uploads encountered issues:");
                ra.addFlashAttribute("errorMessage", String.join("; ", uploadErrors));
            } else {
                ra.addFlashAttribute("successMessage",
                        "✅ PTO " + response.getPtoNumber() + " created successfully for " + response.getPtoHolderName() +
                                ". Documents uploaded: ID Copy, TA Letter, Site Sketch.");
            }
            return "redirect:/ptos/" + ptoId;

        } catch (Exception e) {
            log.error("Error creating PTO: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "❌ Error creating PTO: " + e.getMessage());
            return "redirect:/ptos/create";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            // Chiefs/headsmen may only view PTOs of their own authority
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                requirePtoAccess(id);
            }

            var pto = ptoService.findById(id);
            var documents = documentService.findByRelatedEntity(EntityType.PTO, id);
            model.addAttribute("pto", pto);
            model.addAttribute("documents", documents);
            model.addAttribute("pageTitle", "PTO " + pto.getPtoNumber());
            model.addAttribute("currentPage", "ptos");
            return "ptos/detail";
        } catch (Exception e) {
            log.error("Error loading PTO detail: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/ptos";
        }
    }

    @PostMapping("/{id}/approve")
    public String approve(
            @PathVariable Long id,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {
        try {
            // Chiefs may only approve PTOs of their own authority
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                requirePtoAccess(id);
            }

            var request = PTOApprovalRequest.builder().notes(notes).build();
            var response = ptoService.approvePTO(id, request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage", "✅ PTO " + response.getPtoNumber() + " approved successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/ptos/" + id;
    }

    @PostMapping("/{id}/suspend")
    public String suspendPTO(
            @PathVariable Long id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                requirePtoAccess(id);
            }

            var response = ptoService.suspendPTO(id, reason, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ PTO " + response.getPtoNumber() + " suspended successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/ptos/" + id;
    }

    @PostMapping("/{id}/reactivate")
    public String reactivatePTO(
            @PathVariable Long id,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                requirePtoAccess(id);
            }

            var response = ptoService.reactivatePTO(id, notes, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ PTO " + response.getPtoNumber() + " reactivated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/ptos/" + id;
    }

    @PostMapping("/{id}/revoke")
    public String revoke(
            @PathVariable Long id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {
        try {
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                requirePtoAccess(id);
            }

            var request = PTORevokeRequest.builder().reason(reason).build();
            var response = ptoService.revokePTO(id, request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage", "✅ PTO " + response.getPtoNumber() + " revoked successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/ptos/" + id;
    }

    @PostMapping("/{id}/reinstate")
    public String reinstatePTO(
            @PathVariable Long id,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {
        try {
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                requirePtoAccess(id);
            }

            ptoService.reinstate(id, reason);
            redirectAttributes.addFlashAttribute("successMessage", "✅ PTO reinstated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/ptos/" + id;
    }

    @GetMapping("/by-authority/{authorityId}")
    public String byAuthority(@PathVariable Long authorityId, @RequestParam(required = false, defaultValue = "1") Integer page, Model model, RedirectAttributes ra) {
        try {
            // Chiefs/headsmen may only view PTOs of their own authority
            if (scopeService.isCurrentUserChiefOrHeadsman()
                    && !scopeService.canAccessAuthority(authorityId)) {
                ra.addFlashAttribute("errorMessage",
                        "You are not authorized to view these PTOs.");
                return "redirect:/ptos";
            }


            var authority = authorityService.findById(authorityId);
            Pageable pageable = PageRequestUtils.toPageable(page, 10);
            Page<PTOResponse> pageObj = ptoService.findByAuthority(authorityId, pageable);
            model.addAttribute("page", pageObj);
            model.addAttribute("ptos", pageObj.getContent());

            model.addAttribute("authority", authority);
            model.addAttribute("statuses", PTOStatus.values());
            model.addAttribute("purposes", PTOPurpose.values());
            model.addAttribute("pageTitle", "PTOs — " + authority.getAuthorityName());
            model.addAttribute("currentPage", "ptos");
            return "ptos/list";
        } catch (Exception e) {
            log.error("Error loading PTOs by authority: {}", e.getMessage(), e);
            return "redirect:/ptos";
        }
    }

    @GetMapping("/villages/{authorityId}")
    @ResponseBody
    public Object getVillagesByAuthority(@PathVariable Long authorityId) {
        try {
            // Chiefs/headsmen may only load villages they are scoped to.
            // They may be scoped via a linked authority, or directly via the
            // villages they head. Allow the authority's villages if at least
            // one is within the user's scoped set.
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                if (scopeService.canAccessAuthority(authorityId)) {
                    log.info("Loading villages for authority ID: {}", authorityId);
                    return villageService.findByAuthority(authorityId);
                }
                Set<Long> scopedVillageIds = scopeService.scopedVillageIds();
                if (scopedVillageIds == null || scopedVillageIds.isEmpty()) {
                    return Collections.emptyList();
                }
                log.info("Loading scoped villages for authority ID: {}", authorityId);
                return villageService.findByAuthority(authorityId).stream()
                        .filter(v -> scopedVillageIds.contains(v.getId()))
                        .collect(Collectors.toList());
            }

            log.info("Loading villages for authority ID: {}", authorityId);
            var villages = villageService.findByAuthority(authorityId);
            log.info("Found {} villages for authority {}", villages.size(), authorityId);
            return villages;
        } catch (Exception e) {
            log.error("Error loading villages for authority {}: {}", authorityId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            // Chiefs/headsmen may only edit PTOs of their own authority
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                requirePtoAccess(id);
            }

            var pto = ptoService.findById(id);

            var form = PTORequest.builder()
                    .ptoHolderName(pto.getPtoHolderName())
                    .idNumber(pto.getIdNumber())
                    .contactPhone(pto.getContactPhone())
                    .contactEmail(pto.getContactEmail())
                    .purpose(pto.getPurpose().name())
                    .issueDate(pto.getIssueDate())
                    .expiryDate(pto.getExpiryDate())
                    .notes(pto.getNotes())
                    .villageId(pto.getVillageId())
                    .traditionalAuthorityId(pto.getTraditionalAuthorityId())
                    .allocatedBy(pto.getAllocatedBy())
                    .allocationDate(pto.getAllocationDate())
                    .standArea(pto.getStandArea())
                    .surveyReference(pto.getSurveyReference())
                    .boundaryDescription(pto.getBoundaryDescription())
                    .allocationFeeReceipt(pto.getAllocationFeeReceipt())
                    .taRecommendationRef(pto.getTaRecommendationRef())
                    .communityResolutionRequired(pto.getCommunityResolutionRequired())
                    .build();

                        List<TraditionalAuthorityResponse> authorities;
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                // Every authority the user belongs to; fall back to the PTO's
                // own authority so a village-scoped headsman can still edit.
                java.util.Set<Long> allowed = new java.util.HashSet<>(
                        scopeService.getCurrentUserAuthorityIds());
                if (allowed.isEmpty() && pto.getTraditionalAuthorityId() != null) {
                    allowed.add(pto.getTraditionalAuthorityId());
                }
                authorities = allowed.isEmpty()
                        ? Collections.emptyList()
                        : authorityService.findAllActive().stream()
                            .filter(a -> allowed.contains(a.getId()))
                            .toList();
            } else {
                authorities = authorityService.findAllActive();
            }
            var villages = villageService.findByAuthority(pto.getTraditionalAuthorityId());

            model.addAttribute("pto", pto);
            model.addAttribute("form", form);
            model.addAttribute("authorities", authorities);
            model.addAttribute("villages", villages);
            model.addAttribute("purposes", PTOPurpose.values());
            model.addAttribute("pageTitle", "Edit PTO " + pto.getPtoNumber());
            model.addAttribute("currentPage", "ptos");
            return "ptos/edit";
        } catch (Exception e) {
            log.error("Error loading edit form: {}", e.getMessage(), e);
            return "redirect:/ptos/" + id;
        }
    }

    @PostMapping("/{id}/edit")
    public String updatePTO(
            @PathVariable Long id,
            @RequestParam String ptoHolderName,
            @RequestParam String idNumber,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) String contactEmail,
            @RequestParam String purpose,
            @RequestParam String issueDate,
            @RequestParam(required = false) String expiryDate,
            @RequestParam(required = false) String notes,
            @RequestParam Long villageId,
            @RequestParam Long traditionalAuthorityId,
            @RequestParam(required = false) String allocatedBy,
            @RequestParam(required = false) String allocationDate,
            @RequestParam(required = false) Double standArea,
            @RequestParam(required = false) String surveyReference,
            @RequestParam(required = false) String boundaryDescription,
            @RequestParam(required = false) String allocationFeeReceipt,
            @RequestParam(required = false) String taRecommendationRef,
            @RequestParam(required = false) Boolean communityResolutionRequired,
            @RequestParam(required = false) MultipartFile idCopy,
            @RequestParam(required = false) MultipartFile taAllocationLetter,
            @RequestParam(required = false) MultipartFile siteSketch,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
            // Chiefs/headsmen may only update PTOs of their own authority
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                requirePtoAccess(id);
                scopeService.requireAuthorityAccess(Long.valueOf(traditionalAuthorityId));
            }

            var request = PTORequest.builder()
                    .ptoHolderName(ptoHolderName)
                    .idNumber(idNumber)
                    .contactPhone(contactPhone)
                    .contactEmail(contactEmail)
                    .purpose(purpose)
                    .issueDate(LocalDate.parse(issueDate))
                    .expiryDate(expiryDate != null && !expiryDate.isBlank() ? LocalDate.parse(expiryDate) : null)
                    .notes(notes)
                    .villageId(villageId)
                    .traditionalAuthorityId(traditionalAuthorityId)
                    .allocatedBy(allocatedBy)
                    .allocationDate(allocationDate != null && !allocationDate.isBlank() ? LocalDate.parse(allocationDate) : null)
                    .standArea(standArea)
                    .surveyReference(surveyReference)
                    .boundaryDescription(boundaryDescription)
                    .allocationFeeReceipt(allocationFeeReceipt)
                    .taRecommendationRef(taRecommendationRef)
                    .communityResolutionRequired(communityResolutionRequired)
                    .build();

            var response = ptoService.updatePTO(id, request, userDetails.getUsername());

            // Upload additional documents if provided
            if (idCopy != null && !idCopy.isEmpty()) {
                try {
                    documentService.uploadDocument(idCopy,
                            new za.co.taloms.document.application.dto.DocumentUploadRequest(
                                    za.co.taloms.document.domain.entity.DocumentType.ID_COPY.name(),
                                    za.co.taloms.document.domain.entity.EntityType.PTO.name(),
                                    id,
                                    "ID / Passport Copy — " + response.getPtoHolderName(),
                                    "Uploaded during PTO edit"),
                            userDetails.getUsername(), "127.0.0.1", "Web UI");
                } catch (Exception e) {
                    log.warn("ID copy upload failed for PTO {}: {}", id, e.getMessage());
                }
            }
            if (taAllocationLetter != null && !taAllocationLetter.isEmpty()) {
                try {
                    documentService.uploadDocument(taAllocationLetter,
                            new za.co.taloms.document.application.dto.DocumentUploadRequest(
                                    za.co.taloms.document.domain.entity.DocumentType.TA_ALLOCATION_LETTER.name(),
                                    za.co.taloms.document.domain.entity.EntityType.PTO.name(),
                                    id,
                                    "TA Allocation Letter — " + response.getPtoNumber(),
                                    "Uploaded during PTO edit"),
                            userDetails.getUsername(), "127.0.0.1", "Web UI");
                } catch (Exception e) {
                    log.warn("TA allocation letter upload failed for PTO {}: {}", id, e.getMessage());
                }
            }
            if (siteSketch != null && !siteSketch.isEmpty()) {
                try {
                    documentService.uploadDocument(siteSketch,
                            new za.co.taloms.document.application.dto.DocumentUploadRequest(
                                    za.co.taloms.document.domain.entity.DocumentType.SITE_SKETCH.name(),
                                    za.co.taloms.document.domain.entity.EntityType.PTO.name(),
                                    id,
                                    "Site Sketch — PTO " + response.getPtoNumber(),
                                    "Uploaded during PTO edit"),
                            userDetails.getUsername(), "127.0.0.1", "Web UI");
                } catch (Exception e) {
                    log.warn("Site sketch upload failed for PTO {}: {}", id, e.getMessage());
                }
            }

            ra.addFlashAttribute("successMessage", "✅ PTO updated successfully.");
            return "redirect:/ptos/" + id;
        } catch (Exception e) {
            log.error("Error updating PTO: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "Error updating PTO: " + e.getMessage());
            return "redirect:/ptos/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deletePTO(
            @PathVariable Long id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                requirePtoAccess(id);
            }

            ptoService.deletePTO(id, userDetails.getUsername());
            ra.addFlashAttribute("successMessage", "✅ PTO deleted successfully. Record preserved for audit trail.");
            return "redirect:/ptos/deleted";
        } catch (Exception e) {
            log.error("Error deleting PTO: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "Error deleting PTO: " + e.getMessage());
            return "redirect:/ptos/" + id;
        }
    }

    @GetMapping("/deleted")
    public String deletedList(Model model, @RequestParam(required = false, defaultValue = "1") Integer page, RedirectAttributes ra) {
        try {

            boolean scopedUser = scopeService.isCurrentUserChiefOrHeadsman();
            Set<Long> scopedVillageIds = scopedUser ? scopeService.scopedVillageIds() : null;
            
            Pageable pageable = PageRequestUtils.toPageable(page, 10);
            Page<PTOResponse> pageObj;
            
            if (scopedUser) {
                if (scopedVillageIds == null || scopedVillageIds.isEmpty()) {
                    pageObj = new org.springframework.data.domain.PageImpl<>(Collections.emptyList(), pageable, 0);
                } else {
                    pageObj = ptoService.findDeletedScoped(scopedVillageIds, pageable);
                }
            } else {
                pageObj = ptoService.findDeleted(pageable);
            }

            model.addAttribute("page", pageObj);
            model.addAttribute("ptos", pageObj.getContent());
            model.addAttribute("pageTitle", "Deleted PTOs");

            model.addAttribute("currentPage", "ptos");
            return "ptos/deleted";
        } catch (Exception e) {
            log.error("Error loading deleted PTOs: {}", e.getMessage(), e);
            return "redirect:/ptos";
        }
    }
}

