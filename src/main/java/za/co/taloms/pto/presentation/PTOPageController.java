package za.co.taloms.pto.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.pto.application.dto.*;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.pto.domain.entity.PTOPurpose;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;
import java.time.LocalDate;
import java.util.Collections;

@Slf4j
@Controller
@RequestMapping("/ptos")
@RequiredArgsConstructor
public class PTOPageController {

    private final PTOService ptoService;
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

            model.addAttribute("authorities", authorities);
            model.addAttribute("purposes", PTOPurpose.values());
            model.addAttribute("pageTitle", "Create PTO");
            model.addAttribute("currentPage", "ptos");
            return "ptos/create";
        } catch (Exception e) {
            log.error("Error loading create PTO form: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading form: " + e.getMessage());
            model.addAttribute("authorities", Collections.emptyList());
            model.addAttribute("purposes", PTOPurpose.values());
            model.addAttribute("pageTitle", "Create PTO");
            model.addAttribute("currentPage", "ptos");
            return "ptos/create";
        }
    }

    @PostMapping("/create")
    public String create(
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

        log.info("Creating PTO - Holder: {}, ID: {}, Purpose: {}, Village: {}, Authority: {}",
                ptoHolderName, idNumber, purpose, villageId, traditionalAuthorityId);

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

            var response = ptoService.createPTO(request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ PTO " + response.getPtoNumber() + " created successfully for " + response.getPtoHolderName() + ".");
            return "redirect:/ptos";

        } catch (Exception e) {
            log.error("Error creating PTO: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "❌ Error creating PTO: " + e.getMessage());
            ra.addFlashAttribute("form", PTORequest.builder()
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
                    .build());
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
            model.addAttribute("totalCount", 0L);
            model.addAttribute("pendingCount", 0L);
            model.addAttribute("activeCount", 0L);
            model.addAttribute("revokedCount", 0L);
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

    // Add to PTOPageController.java

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        try {
            var pto = ptoService.findById(id);

            // Create a request object with existing values
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
            // You need to add an update method to your service
            // For now, let's just redirect with a message
            ra.addFlashAttribute("infoMessage",
                    "Edit functionality coming soon. Please use the PTO list to manage records.");
            return "redirect:/ptos/" + id;
        } catch (Exception e) {
            log.error("Error updating PTO: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "Error updating PTO: " + e.getMessage());
            return "redirect:/ptos/" + id;
        }
    }
}