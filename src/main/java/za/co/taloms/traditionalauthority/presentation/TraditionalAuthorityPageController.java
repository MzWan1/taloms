package za.co.taloms.traditionalauthority.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.traditionalauthority.application.dto.*;
import za.co.taloms.traditionalauthority.application.service.*;
import za.co.taloms.security.application.service.AuthorityScopeService;
import java.util.List;

@Controller
@RequestMapping("/authorities")
@RequiredArgsConstructor
@Slf4j
public class TraditionalAuthorityPageController {

    private final TraditionalAuthorityService authorityService;
    private final VillageService              villageService;
    private final AuthorityScopeService       scopeService;

    @GetMapping
    public String list(Model model) {
        List<TraditionalAuthorityResponse> authorities;
        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            // Chiefs/headsmen see every authority that belongs to them
            java.util.Set<Long> linkedIds = scopeService.getCurrentUserAuthorityIds();
            authorities = linkedIds.isEmpty()
                    ? java.util.Collections.emptyList()
                    : authorityService.findAll().stream()
                        .filter(a -> linkedIds.contains(a.getId()))
                        .toList();
        } else {
            authorities = authorityService.findAll();
        }
        log.info("AuthoritiesPageController: model 'authorities' size = {}", authorities.size());
        model.addAttribute("authorities", authorities);
        model.addAttribute("pageTitle",   "Traditional Authorities");
        model.addAttribute("currentPage", "authorities");
        return "authorities/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createForm(Model model) {
        // After a failed submit the submitted values (incl. the mapped
        // boundary) arrive here as a flash attribute and refill the form
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new TraditionalAuthorityRequest());
        }
        model.addAttribute("pageTitle",   "Create Authority");
        model.addAttribute("currentPage", "authorities");
        return "authorities/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
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
            // Keep everything the admin typed (including the mapped boundary)
            // so the form can be refilled after the failure
            ra.addFlashAttribute("form", request);
            return "redirect:/authorities/create";
        }
        return "redirect:/authorities";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        var authority = authorityService.findById(id);
        var form = TraditionalAuthorityRequest.builder()
                .authorityName(authority.getAuthorityName())
                .chiefId(authority.getChiefId())
                .headmanId(authority.getHeadmanId())
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    public String detail(@PathVariable Long id,
                         RedirectAttributes ra,
                         Model model) {

        // Chiefs may only view their own authority
        if (!scopeService.canAccessAuthority(id)) {
            ra.addFlashAttribute("errorMessage",
                    "You are not authorized to view this authority.");
            return "redirect:/authorities";
        }

        var authority = authorityService.findById(id);

        // Boundaries (authority + villages) for the map viewer
        model.addAttribute("villageList", villageService.findByAuthority(id));

        model.addAttribute("authority",   authority);
        model.addAttribute("pageTitle",   "Authority Detail");
        model.addAttribute("currentPage", "authorities");

        // Villages are CHIEF-only. Only show the villages block to a CHIEF
        // who is linked to this authority (many-to-many).
        boolean canManageVillages =
                scopeService.canAccessAuthority(id)
                        && isChiefLinkedToAuthority(id);
        model.addAttribute("canAddVillages", canManageVillages);

        // Chiefs linked to this authority (admin can manage the relationship)
        model.addAttribute("chiefs", authorityService.findChiefs(id));
        model.addAttribute("canManageChiefs", scopeService.isCurrentUserAdmin());

        if (canManageVillages) {
            var villages = villageService.findByAuthority(id);

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

            model.addAttribute("villages",          villages);
            model.addAttribute("villageRows",        villageRows);
            model.addAttribute("activeVillageCount", activeCount);
        } else {
            model.addAttribute("villages",          java.util.Collections.emptyList());
            model.addAttribute("villageRows",        java.util.Collections.emptyList());
            model.addAttribute("activeVillageCount", 0);
        }

        return "authorities/detail";
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('CHIEF')")
    public String createVillageForm(
            @PathVariable Long authorityId, Model model) {
        // Validate CHIEF can only add villages to their linked authority
        validateChiefAuthority(authorityId);
        model.addAttribute("form",
                VillageRequest.builder()
                        .traditionalAuthorityId(authorityId)
                        .build());
        model.addAttribute("authority",
                authorityService.findById(authorityId));
        model.addAttribute("myAuthorities", currentUserAuthorities());
        model.addAttribute("pageTitle",   "Add Village");
        model.addAttribute("currentPage", "authorities");
        return "authorities/village-form";
    }

    @PostMapping("/{authorityId}/villages/create")
    @PreAuthorize("hasRole('CHIEF')")
    public String createVillage(
            @PathVariable Long authorityId,
            @ModelAttribute("form") VillageRequest request,
            Model model,
            RedirectAttributes ra) {
        try {
            // Validate CHIEF can only add villages to their linked authority
            validateChiefAuthority(authorityId);
            request.setTraditionalAuthorityId(authorityId);
            villageService.create(request);
            ra.addFlashAttribute("successMessage",
                    "Village '" + request.getVillageName()
                            + "' added successfully.");
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("form", request);
            model.addAttribute("authority",
                    authorityService.findById(authorityId));
            model.addAttribute("myAuthorities", currentUserAuthorities());
            model.addAttribute("pageTitle",   "Add Village");
            model.addAttribute("currentPage", "authorities");
            return "authorities/village-form";
        }
        return "redirect:/authorities/" + authorityId;
    }

    @PostMapping("/villages/{id}/deactivate")
    @PreAuthorize("hasRole('CHIEF')")
    public String deactivateVillage(
            @PathVariable Long id, RedirectAttributes ra) {
        var village = villageService.findById(id);
        // Validate CHIEF can only modify villages in their linked authority
        validateChiefAuthority(village.getTraditionalAuthorityId());
        villageService.deactivate(id);
        ra.addFlashAttribute("successMessage",
                "Village deactivated successfully.");
        return "redirect:/authorities/"
                + village.getTraditionalAuthorityId();
    }

    @PostMapping("/villages/{id}/activate")
    @PreAuthorize("hasRole('CHIEF')")
    public String activateVillage(
            @PathVariable Long id, RedirectAttributes ra) {
        var village = villageService.findById(id);
        // Validate CHIEF can only modify villages in their linked authority
        validateChiefAuthority(village.getTraditionalAuthorityId());
        villageService.activate(id);
        ra.addFlashAttribute("successMessage",
                "Village activated successfully.");
        return "redirect:/authorities/"
                + village.getTraditionalAuthorityId();
    }

    // ── Chief ↔ Authority management (ADMIN) ──────────────────────────

    @PostMapping("/{id}/chiefs/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String addChiefToAuthority(
            @PathVariable Long id,
            @RequestParam("chiefId") Long chiefId,
            RedirectAttributes ra) {
        try {
            authorityService.addChiefToAuthority(id, chiefId);
            ra.addFlashAttribute("successMessage",
                    "Chief linked to this authority successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/authorities/" + id;
    }

    @PostMapping("/{id}/chiefs/{chiefId}/remove")
    @PreAuthorize("hasRole('ADMIN')")
    public String removeChiefFromAuthority(
            @PathVariable Long id,
            @PathVariable Long chiefId,
            RedirectAttributes ra) {
        try {
            authorityService.removeChiefFromAuthority(id, chiefId);
            ra.addFlashAttribute("successMessage",
                    "Chief removed from this authority successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/authorities/" + id;
    }

    /**
     * The authorities the current user may add villages to, in a stable order.
     * Used by the Add Village form so the authority context is always explicit.
     */
    private List<TraditionalAuthorityResponse> currentUserAuthorities() {
        java.util.Set<Long> ids = scopeService.getCurrentUserAuthorityIds();
        if (ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return authorityService.findAllActive().stream()
                .filter(a -> ids.contains(a.getId()))
                .toList();
    }

    /**
     * Validates that the current user (CHIEF) is linked to the given authority.
     * Throws SecurityException if not authorized.
     */
    private void validateChiefAuthority(Long authorityId) {
        scopeService.requireAuthorityAccess(authorityId);
    }

    /**
     * Returns true only for a CHIEF user linked to the given authority through
     * the many-to-many chief_authorities relationship.
     * Administrators are deliberately NOT allowed here (villages are chief-only).
     */
    private boolean isChiefLinkedToAuthority(Long authorityId) {
        var user = scopeService.getCurrentUser();
        if (user == null) {
            return false;
        }
        boolean isChief = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_CHIEF"));
        if (!isChief) {
            return false;
        }
        return scopeService.getCurrentUserAuthorityIds().contains(authorityId);
    }
}


