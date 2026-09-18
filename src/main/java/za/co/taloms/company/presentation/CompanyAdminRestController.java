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

import java.util.List;

/**
 * Administrative management of external companies.
 *
 * ADMIN-only at both the URL layer (SecurityConfig) and the method layer. The
 * COMPANY role can never reach these endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CompanyAdminRestController {

    private final CompanyService companyService;
    private final CompanyApiKeyService apiKeyService;
    private final ApiUsageService usageService;

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyResponse>> create(
            @Valid @RequestBody CompanyCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        CompanyResponse created = companyService.createCompany(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Company registered successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                companyService.findAll(), "Companies retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                companyService.findById(id), "Company retrieved successfully"));
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<CompanyResponse>> disable(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ReasonRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(ApiResponse.success(
                companyService.disableCompany(id, reason, userDetails.getUsername()),
                "Company API access disabled"));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<CompanyResponse>> activate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                companyService.activateCompany(id, userDetails.getUsername()),
                "Company API access enabled"));
    }

    @PostMapping("/{id}/keys")
    public ResponseEntity<ApiResponse<CompanyApiKeyResponse>> generateKey(
            @PathVariable Long id,
            @Valid @RequestBody CompanyApiKeyCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        // The raw key is present in THIS response only.
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                apiKeyService.generateKey(id, request, userDetails.getUsername()),
                "API key generated. Copy it now — it will not be shown again."));
    }

    @GetMapping("/{id}/keys")
    public ResponseEntity<ApiResponse<List<CompanyApiKeySummaryResponse>>> getKeys(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                apiKeyService.findByCompany(id), "API keys retrieved successfully"));
    }

    @PatchMapping("/{id}/keys/{keyId}/revoke")
    public ResponseEntity<ApiResponse<CompanyApiKeySummaryResponse>> revokeKey(
            @PathVariable Long id,
            @PathVariable Long keyId,
            @Valid @RequestBody(required = false) ReasonRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(ApiResponse.success(
                apiKeyService.revokeKey(id, keyId, reason, userDetails.getUsername()),
                "API key revoked"));
    }

    @GetMapping("/{id}/usage")
    public ResponseEntity<ApiResponse<List<ApiUsageLogResponse>>> getUsage(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                usageService.findByCompany(id), "API usage retrieved successfully"));
    }
}