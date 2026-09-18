package za.co.taloms.company.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.company.application.dto.*;
import za.co.taloms.company.application.service.ApiUsageService;
import za.co.taloms.company.application.service.CompanyApiKeyService;
import za.co.taloms.company.application.service.CompanyService;
import za.co.taloms.company.domain.entity.ApiScope;

import java.util.List;

/**
 * Self-service endpoints for companies authenticated via form login (ROLE_COMPANY).
 *
 * These are separate from the external API (CompanyApiController) which uses
 * API-key authentication. Companies can view their profile, manage their own
 * API keys, and check usage via the web UI.
 */
@Slf4j
@RestController
@RequestMapping("/api/company/self")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY')")
public class CompanySelfRestController {

    private final CompanyService companyService;
    private final CompanyApiKeyService apiKeyService;
    private final ApiUsageService usageService;

    @GetMapping("/company")
    public ResponseEntity<ApiResponse<CompanyResponse>> getMyCompany(
            @AuthenticationPrincipal UserDetails userDetails) {
        CompanyResponse company = companyService.findByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(company, "Company profile retrieved"));
    }

    @GetMapping("/keys")
    public ResponseEntity<ApiResponse<List<CompanyApiKeySummaryResponse>>> getMyKeys(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long companyId = companyService.findByUsername(userDetails.getUsername()).getId();
        List<CompanyApiKeySummaryResponse> keys = apiKeyService.findByCompany(companyId);
        return ResponseEntity.ok(ApiResponse.success(keys, "API keys retrieved"));
    }

    @PostMapping("/keys")
    public ResponseEntity<ApiResponse<CompanyApiKeyResponse>> generateKey(
            @Valid @RequestBody CompanyApiKeyCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long companyId = companyService.findByUsername(userDetails.getUsername()).getId();
        CompanyApiKeyResponse generated = apiKeyService.generateKey(companyId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(generated, "API key generated. Copy it now — it will not be shown again."));
    }

    @PatchMapping("/keys/{keyId}/revoke")
    public ResponseEntity<ApiResponse<CompanyApiKeySummaryResponse>> revokeKey(
            @PathVariable Long keyId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long companyId = companyService.findByUsername(userDetails.getUsername()).getId();
        CompanyApiKeySummaryResponse revoked = apiKeyService.revokeKey(companyId, keyId, reason, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(revoked, "API key revoked"));
    }

    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<List<ApiUsageLogResponse>>> getMyUsage(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long companyId = companyService.findByUsername(userDetails.getUsername()).getId();
        List<ApiUsageLogResponse> usage = usageService.findByCompany(companyId);
        return ResponseEntity.ok(ApiResponse.success(usage, "Usage records retrieved"));
    }
}