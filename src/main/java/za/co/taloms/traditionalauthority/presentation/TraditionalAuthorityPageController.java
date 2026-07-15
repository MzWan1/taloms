package za.co.taloms.traditionalauthority.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.traditionalauthority.application.dto.*;
import za.co.taloms.traditionalauthority.application.service.*;

@Controller
@RequestMapping("/authorities")
@RequiredArgsConstructor
public class TraditionalAuthorityPageController {

    private final TraditionalAuthorityService authorityService;
    private final VillageService              villageService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("authorities", authorityService.findAll());
        model.addAttribute("pageTitle",   "Traditional Authorities");
        model.addAttribute("currentPage", "authorities");
        return "authorities/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public String createForm(Model model) {
        model.addAttribute("form",        new TraditionalAuthorityRequest());
        model.addAttribute("pageTitle",   "Create Authority");
        model.addAttribute("currentPage", "authorities");
        return "authorities/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public String create(
            @ModelAttribute("form") TraditionalAuthorityRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {
        try {
            authorityService.create(request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "Traditional Authority '" + request.getAuthorityName()
                            + "' created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/authorities/create";
        }
        return "redirect:/authorities";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public String editForm(@PathVariable Long id, Model model) {
        var authority = authorityService.findById(id);
        var form = TraditionalAuthorityRequest.builder()
                .authorityName(authority.getAuthorityName())
                .chiefName(authority.getChiefName())
                .headmanName(authority.getHeadmanName())
                .contactPhone(authority.getContactPhone())
                .contactEmail(authority.getContactEmail())
                .physicalAddress(authority.getPhysicalAddress())
                .region(authority.getRegion())
                .build();
        model.addAttribute("form",        form);
        model.addAttribute("authorityId", id);
        model.addAttribute("authority",   authority);
        model.addAttribute("villages",    villageService.findByAuthority(id));
        model.addAttribute("pageTitle",   "Edit Authority");
        model.addAttribute("currentPage", "authorities");
        return "authorities/edit";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public String edit(
            @PathVariable Long id,
            @ModelAttribute("form") TraditionalAuthorityRequest request,
            RedirectAttributes ra) {
        try {
            authorityService.update(id, request);
            ra.addFlashAttribute("successMessage",
                    "Authority updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/authorities";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {

        var authority = authorityService.findById(id);
        var villages  = villageService.findByAuthority(id);

        long activeCount = 0;

        // Build plain Map rows — no Boolean evaluation in template
        var villageRows = new java.util.ArrayList<java.util.Map<String,String>>();
        for (var v : villages) {
            boolean isActive = Boolean.TRUE.equals(v.getActive());
            if (isActive) activeCount++;

            var row = new java.util.LinkedHashMap<String, String>();
            row.put("name",        v.getVillageName());
            row.put("initials",    v.getVillageName().length() >= 2
                    ? v.getVillageName().substring(0,2).toUpperCase()
                    : v.getVillageName().toUpperCase());
            row.put("headman",     v.getHeadmanName()  != null ? v.getHeadmanName()  : "—");
            row.put("region",      v.getRegion()       != null ? v.getRegion()       : "—");
            row.put("statusLabel", isActive ? "Active" : "Inactive");
            row.put("statusClass", isActive ? "bg-success" : "bg-secondary");
            row.put("registered",  v.getCreatedAt() != null
                    ? v.getCreatedAt().toLocalDate().toString()
                    : "—");
            villageRows.add(row);
        }

        model.addAttribute("authority",          authority);
        model.addAttribute("villageRows",        villageRows);
        model.addAttribute("activeVillageCount", activeCount);
        model.addAttribute("pageTitle",          "Authority Detail");
        model.addAttribute("currentPage",        "authorities");
        return "authorities/detail";
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public String deactivate(
            @PathVariable Long id, RedirectAttributes ra) {
        try {
            authorityService.deactivate(id);
            ra.addFlashAttribute("successMessage",
                    "Authority deactivated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/authorities";
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public String activate(
            @PathVariable Long id, RedirectAttributes ra) {
        try {
            authorityService.activate(id);
            ra.addFlashAttribute("successMessage",
                    "Authority activated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/authorities";
    }

    // ── Village sub-routes ────────────────────────────────────────────

    @GetMapping("/{authorityId}/villages/create")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public String createVillageForm(
            @PathVariable Long authorityId, Model model) {
        model.addAttribute("form",
                VillageRequest.builder()
                        .traditionalAuthorityId(authorityId)
                        .build());
        model.addAttribute("authority",
                authorityService.findById(authorityId));
        model.addAttribute("authorities",
                authorityService.findAllActive());
        model.addAttribute("pageTitle",   "Add Village");
        model.addAttribute("currentPage", "authorities");
        return "authorities/village-form";
    }

    @PostMapping("/{authorityId}/villages/create")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public String createVillage(
            @PathVariable Long authorityId,
            @ModelAttribute("form") VillageRequest request,
            RedirectAttributes ra) {
        try {
            request.setTraditionalAuthorityId(authorityId);
            villageService.create(request);
            ra.addFlashAttribute("successMessage",
                    "Village '" + request.getVillageName()
                            + "' added successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/authorities/" + authorityId + "/edit";
    }

    @PostMapping("/villages/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public String deactivateVillage(
            @PathVariable Long id, RedirectAttributes ra) {
        var village = villageService.findById(id);
        villageService.deactivate(id);
        ra.addFlashAttribute("successMessage",
                "Village deactivated successfully.");
        return "redirect:/authorities/"
                + village.getTraditionalAuthorityId() + "/edit";
    }

    @PostMapping("/villages/{id}/activate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public String activateVillage(
            @PathVariable Long id, RedirectAttributes ra) {
        var village = villageService.findById(id);
        villageService.activate(id);
        ra.addFlashAttribute("successMessage",
                "Village activated successfully.");
        return "redirect:/authorities/"
                + village.getTraditionalAuthorityId() + "/edit";
    }
}