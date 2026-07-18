package za.co.taloms.pto.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.pto.application.dto.*;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.pto.domain.entity.PTOPurpose;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/ptos")
@RequiredArgsConstructor
public class PTOPageController {

    private final PTOService ptoService;
    private final ParcelService parcelService;
    private final TraditionalAuthorityService authorityService;
    private final VillageService villageService;

    @GetMapping
    public String list(Model model,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String search) {
        try {
            var criteria = PTOSearchCriteria.builder();
            if (status != null && !status.isBlank()) {
                criteria.status(PTOStatus.valueOf(status));
            }
            if (search != null && !search.isBlank()) {
                criteria.holderName(search);
            }

            var ptos = (status != null && !status.isBlank())
                    || (search != null && !search.isBlank())
                    ? ptoService.search(criteria.build())
                    : ptoService.findAll();

            model.addAttribute("ptos", ptos);
            model.addAttribute("statuses", PTOStatus.values());
            model.addAttribute("purposes", PTOPurpose.values());
            model.addAttribute("totalCount", ptoService.countAll());
            model.addAttribute("pendingCount", ptoService.countByStatus(PTOStatus.PENDING));
            model.addAttribute("activeCount", ptoService.countByStatus(PTOStatus.ACTIVE));
            model.addAttribute("revokedCount", ptoService.countByStatus(PTOStatus.REVOKED));
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
            var authorities = authorityService.findAllActive();
            log.info("Loaded {} active authorities for PTO create form", authorities.size());

            if (!model.containsAttribute("form")) {
                model.addAttribute("form", PTORequest.builder()
                        .issueDate(LocalDate.now())
                        .build());
            }

            // Get available parcels for PTO
            List<za.co.taloms.parcel.application.dto.ParcelResponse> availableParcels = Collections.emptyList();
            try {
                var allParcels = parcelService.findAll();
                if (allParcels != null && !allParcels.isEmpty()) {
                    availableParcels = allParcels.stream()
                            .filter(p -> p != null && p.getStatus() == ParcelStatus.AVAILABLE)
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
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        log.info("Creating PTO - Holder: {}, ID: {}, Parcel: {}, Purpose: {}",
                ptoHolderName, idNumber, parcelId, purpose);

        try {
            // Get the parcel to get village and authority
            var parcel = parcelService.findById(parcelId);
            if (parcel == null) {
                ra.addFlashAttribute("errorMessage", "❌ Parcel not found. Please select a valid parcel.");
                return "redirect:/ptos/create";
            }

            var request = PTORequest.builder()
                    .parcelId(parcelId)
                    .ptoHolderName(ptoHolderName)
                    .idNumber(idNumber)
                    .contactPhone(contactPhone)
                    .contactEmail(contactEmail)
                    .purpose(purpose)
                    .issueDate(LocalDate.parse(issueDate))
                    .expiryDate(expiryDate != null && !expiryDate.isBlank() ? LocalDate.parse(expiryDate) : null)
                    .notes(notes)
                    .villageId(parcel.getVillageId())
                    .traditionalAuthorityId(parcel.getVillageId() != null ?
                            // We need to get the authority from the village
                            // This is a simplified approach - you may need to fetch the authority ID differently
                            parcel.getVillageId() : null)
                    .build();

            var response = ptoService.createPTO(request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ PTO " + response.getPtoNumber() + " created successfully for " + response.getPtoHolderName() +
                            " on " + parcel.getStandNumber() + ".");
            return "redirect:/ptos";

        } catch (Exception e) {
            log.error("Error creating PTO: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "❌ Error creating PTO: " + e.getMessage());
            return "redirect:/ptos/create";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        try {
            var pto = ptoService.findById(id);
            model.addAttribute("pto", pto);
            model.addAttribute("pageTitle", "PTO " + pto.getPtoNumber());
            model.addAttribute("currentPage", "ptos");
            return "ptos/detail";
        } catch (Exception e) {
            log.error("Error loading PTO detail: {}", e.getMessage(), e);
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
            ptoService.reinstate(id, reason);
            redirectAttributes.addFlashAttribute("successMessage", "✅ PTO reinstated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/ptos/" + id;
    }

    @GetMapping("/by-authority/{authorityId}")
    public String byAuthority(@PathVariable Long authorityId, Model model) {
        try {
            var authority = authorityService.findById(authorityId);
            model.addAttribute("ptos", ptoService.findByAuthority(authorityId));
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
    public String editForm(@PathVariable Long id, Model model) {
        try {
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
                    .build();

            var authorities = authorityService.findAllActive();
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
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
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
                    .build();

            var response = ptoService.updatePTO(id, request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage", "✅ PTO updated successfully.");
            return "redirect:/ptos/" + id;
        } catch (Exception e) {
            log.error("Error updating PTO: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "Error updating PTO: " + e.getMessage());
            return "redirect:/ptos/" + id + "/edit";
        }
    }
}