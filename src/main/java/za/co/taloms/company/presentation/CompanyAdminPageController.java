package za.co.taloms.company.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.company.application.dto.CompanyApiKeyCreateRequest;
import za.co.taloms.company.application.dto.CompanyApiKeyResponse;
import za.co.taloms.company.application.dto.CompanyCreateRequest;
import za.co.taloms.company.application.service.ApiUsageService;
import za.co.taloms.company.application.service.CompanyApiKeyService;
import za.co.taloms.company.application.service.CompanyService;
import za.co.taloms.company.domain.entity.ApiScope;

import java.util.List;
import java.util.Set;

import za.co.taloms.security.application.service.UserService;

/**
 * Administrator pages for managing externally approved companies.
 *
 * Mirrors the existing TALOMS page-controller style (Thymeleaf + RedirectAttributes).
 * ADMIN-only by method security; the COMPANY role cannot reach these pages.
 */
@Slf4j
@Controller
@RequestMapping("/companies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CompanyAdminPageController {

    private final CompanyService companyService;
    private final CompanyApiKeyService apiKeyService;
    private final ApiUsageService usageService;
    private final UserService userService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("companies", companyService.findAll());
        model.addAttribute("companyForm", new CompanyCreateRequest());
        model.addAttribute("pageTitle", "Companies");
        model.addAttribute("currentPage", "companies");
        return "companies/list";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute("companyForm") CompanyCreateRequest form,
                         RedirectAttributes ra) {
        try {
            var created = companyService.createCompany(form, currentActor());
            ra.addFlashAttribute("successMessage",
                    "Company '" + created.getName() + "' registered successfully.");
            return "redirect:/companies/" + created.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/companies";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("company", companyService.findById(id));
        model.addAttribute("keys", apiKeyService.findByCompany(id));
        // Recent usage for the detail view — bounded page, newest first.
        var usage = usageService.findByCompany(id, 1, 10);
        model.addAttribute("usage", usage.getContent());
        model.addAttribute("usageCount", usage.getTotalElements());
        model.addAttribute("scopes", ApiScope.values());
        model.addAttribute("pageTitle", "Company Detail");
        model.addAttribute("currentPage", "companies");
        return "companies/detail";
    }

    @PostMapping("/{id}/disable")
    public String disable(@PathVariable Long id,
                          @RequestParam(required = false) String reason,
                          RedirectAttributes ra) {
        try {
            companyService.disableCompany(id, reason, currentActor());
            ra.addFlashAttribute("successMessage",
                    "Company disabled. All of its API keys are now blocked.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/companies/" + id;
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes ra) {
        try {
            companyService.activateCompany(id, currentActor());
            ra.addFlashAttribute("successMessage", "Company API access enabled.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/companies/" + id;
    }

    @PostMapping("/{id}/keys")
    public String generateKey(@PathVariable Long id,
                              @RequestParam(required = false) String label,
                              @RequestParam(required = false) Set<ApiScope> scopes,
                              RedirectAttributes ra) {
        try {
            CompanyApiKeyCreateRequest request = CompanyApiKeyCreateRequest.builder()
                    .label(label)
                    .scopes(scopes)
                    .build();
            CompanyApiKeyResponse generated = apiKeyService.generateKey(id, request, currentActor());
            // The raw key is flashed to the detail page and shown exactly once.
            ra.addFlashAttribute("generatedKey", generated.getApiKey());
            ra.addFlashAttribute("generatedKeyPrefix", generated.getKeyPrefix());
            ra.addFlashAttribute("successMessage",
                    "API key generated. Copy it now — it is shown once only.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/companies/" + id;
    }

    @PostMapping("/{id}/keys/{keyId}/revoke")
    public String revokeKey(@PathVariable Long id,
                            @PathVariable Long keyId,
                            @RequestParam(required = false) String reason,
                            RedirectAttributes ra) {
        try {
            apiKeyService.revokeKey(id, keyId, reason, currentActor());
            ra.addFlashAttribute("successMessage", "API key revoked.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/companies/" + id;
    }

    private String currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}