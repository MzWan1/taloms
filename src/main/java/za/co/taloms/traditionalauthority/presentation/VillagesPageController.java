package za.co.taloms.traditionalauthority.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.traditionalauthority.application.service.*;
import za.co.taloms.security.application.service.AuthorityScopeService;

@Controller
@RequestMapping("/villages")
@RequiredArgsConstructor
@Slf4j
public class VillagesPageController {

    private final TraditionalAuthorityService authorityService;
    private final VillageService villageService;
    private final AuthorityScopeService scopeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CHIEF','HEADSMAN')")
    public String list(Model model) {
        // Chiefs/headsmen only see villages of their linked authority
        Long linkedAuthorityId = scopeService.getCurrentUserAuthorityId();
        if (linkedAuthorityId != null) {
            var authority = authorityService.findById(linkedAuthorityId);
            model.addAttribute("authority", authority);
            model.addAttribute("villages", villageService.findByAuthority(linkedAuthorityId));
        } else {
            model.addAttribute("villages", java.util.Collections.emptyList());
        }
        model.addAttribute("pageTitle", "Village Management");
        model.addAttribute("currentPage", "villages");
        return "villages/list";
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('CHIEF')")
    public String deactivate(@PathVariable Long id, RedirectAttributes ra) {
        try {
            var village = villageService.findById(id);
            // CHIEF can only modify villages in their linked authority
            scopeService.requireAuthorityAccess(village.getTraditionalAuthorityId());
            villageService.deactivate(id);
            ra.addFlashAttribute("successMessage",
                    "Village deactivated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/villages";
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('CHIEF')")
    public String activate(@PathVariable Long id, RedirectAttributes ra) {
        try {
            var village = villageService.findById(id);
            // CHIEF can only modify villages in their linked authority
            scopeService.requireAuthorityAccess(village.getTraditionalAuthorityId());
            villageService.activate(id);
            ra.addFlashAttribute("successMessage",
                    "Village activated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/villages";
    }
}