package za.co.taloms.company.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.company.application.dto.*;
import za.co.taloms.company.application.service.ApiUsageService;
import za.co.taloms.company.application.service.CompanyApiKeyService;
import za.co.taloms.company.application.service.CompanyService;
import za.co.taloms.company.domain.entity.ApiKeyStatus;
import za.co.taloms.company.domain.entity.ApiScope;

import java.util.List;

/**
 * Self-service web UI for companies (ROLE_COMPANY).
 *
 * Allows companies to view their profile, manage their own API keys,
 * and check usage statistics after logging in via form authentication.
 */
@Slf4j
@Controller
@RequestMapping("/company")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY')")
public class CompanySelfPageController {

    private final CompanyService companyService;
    private final CompanyApiKeyService apiKeyService;
    private final ApiUsageService usageService;

    @GetMapping
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        CompanyResponse company = companyService.findByUsername(userDetails.getUsername());
        List<CompanyApiKeySummaryResponse> keys = apiKeyService.findByCompany(company.getId());
        List<ApiUsageLogResponse> usage = usageService.findByCompany(company.getId());

        model.addAttribute("company", company);
        model.addAttribute("keys", keys);
        model.addAttribute("usage", usage);
        model.addAttribute("usageCount", usage.size());
        model.addAttribute("scopes", ApiScope.values());
        model.addAttribute("apiKeyForm", new CompanyApiKeyCreateRequest());
        model.addAttribute("pageTitle", "Company Dashboard");
        model.addAttribute("currentPage", "company");
        return "company/dashboard";
    }

    @PostMapping("/keys")
    public String generateKey(@ModelAttribute("apiKeyForm") CompanyApiKeyCreateRequest form,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes ra) {
        try {
            CompanyResponse company = companyService.findByUsername(userDetails.getUsername());
            CompanyApiKeyResponse generated = apiKeyService.generateKey(
                    company.getId(), form, userDetails.getUsername());
            ra.addFlashAttribute("generatedKey", generated.getApiKey());
            ra.addFlashAttribute("generatedKeyPrefix", generated.getKeyPrefix());
            ra.addFlashAttribute("successMessage",
                    "API key generated. Copy it now — it is shown once only.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/company";
    }

    @PostMapping("/keys/{keyId}/revoke")
    public String revokeKey(@PathVariable Long keyId,
                            @RequestParam(required = false) String reason,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes ra) {
        try {
            CompanyResponse company = companyService.findByUsername(userDetails.getUsername());
            apiKeyService.revokeKey(company.getId(), keyId, reason, userDetails.getUsername());
            ra.addFlashAttribute("successMessage", "API key revoked.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/company";
    }
}