package za.co.taloms.company.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.common.PageResponse;
import za.co.taloms.company.application.dto.*;
import za.co.taloms.company.application.service.*;
import za.co.taloms.company.domain.security.CompanyApiPrincipal;
import za.co.taloms.company.infrastructure.security.ApiKeyHasher;

import java.util.List;

/**
 * The protected external API consumed by approved companies.
 *
 * Authentication is performed by
 * {@link CompanyApiKeyAuthenticationFilter} before this controller runs, and
 * authorization is enforced per endpoint with {@code @PreAuthorize} against the
 * key's scopes. Every response is derived from the authenticated
 * {@link CompanyApiPrincipal}, so a company can only ever act on its own data.
 */
@Slf4j
@RestController
@RequestMapping("/api/external")
@RequiredArgsConstructor
public class CompanyApiController {

        private final ProofOfResidenceService proofOfResidenceService;
        private final CompanyService companyService;
        private final CompanyApiKeyService apiKeyService;
        private final ApiUsageService usageService;
        private final ApiRateLimitService rateLimitService;
        private final ApiKeyHasher apiKeyHasher;

        /**
         * Verifies whether the supplied ID number has a valid proof of residence.
         * Requires the {@code POR_READ} scope.
         */
        @PostMapping("/por/verify")
        @PreAuthorize("hasRole('COMPANY') and hasAuthority('SCOPE_POR_READ')")
        public ResponseEntity<ApiResponse<PorVerificationResponse>> verifyProofOfResidence(
                        @Valid @RequestBody PorVerificationRequest request,
                        @AuthenticationPrincipal CompanyApiPrincipal principal,
                        HttpServletRequest httpRequest) {

                // Only the SHA-256 digest is ever exposed to the usage log.
                String idNumberHash = apiKeyHasher.hashIdNumber(request.getIdNumber());
                httpRequest.setAttribute(CompanyApiKeyAuthenticationFilter.ATTR_ID_NUMBER_HASH, idNumberHash);
                httpRequest.setAttribute(CompanyApiKeyAuthenticationFilter.ATTR_SEARCH_TYPE, "ID Number");
                httpRequest.setAttribute(CompanyApiKeyAuthenticationFilter.ATTR_MASKED_SEARCH_VALUE,
                                za.co.taloms.common.IdMasker.maskIdNumber(request.getIdNumber()));

                // Throttle repeated lookups of the same ID number by the same key, which
                // is the main enumeration vector for this endpoint.
                RateLimitDecision decision = rateLimitService.checkIdNumberQuery(principal.apiKeyId(), idNumberHash);
                if (!decision.allowed()) {
                        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                                        "Too many repeated ID number queries. Please retry later.");
                }

                PorVerificationResponse response = proofOfResidenceService.verify(request.getIdNumber());
                return ResponseEntity.ok(ApiResponse.success(response,
                                response.isVerified()
                                                ? "Proof of residence verified"
                                                : "No valid proof of residence found"));
        }

        /** The calling company's own profile. Requires {@code COMPANY_SELF_READ}. */
        @GetMapping("/self/company")
        @PreAuthorize("hasRole('COMPANY') and hasAuthority('SCOPE_COMPANY_SELF_READ')")
        public ResponseEntity<ApiResponse<CompanyResponse>> myCompany(
                        @AuthenticationPrincipal CompanyApiPrincipal principal) {
                return ResponseEntity.ok(ApiResponse.success(
                                companyService.findById(principal.companyId()),
                                "Company profile retrieved successfully"));
        }

        /** The calling company's own API keys (never their secret material). */
        @GetMapping("/self/keys")
        @PreAuthorize("hasRole('COMPANY') and hasAuthority('SCOPE_COMPANY_SELF_READ')")
        public ResponseEntity<ApiResponse<List<CompanyApiKeySummaryResponse>>> myKeys(
                        @AuthenticationPrincipal CompanyApiPrincipal principal) {
                return ResponseEntity.ok(ApiResponse.success(
                                apiKeyService.findByCompany(principal.companyId()),
                                "API keys retrieved successfully"));
        }

        /** The calling company's own usage records — never another company's. */
        @GetMapping("/self/usage")
        @PreAuthorize("hasRole('COMPANY') and hasAuthority('SCOPE_COMPANY_SELF_READ')")
        public ResponseEntity<ApiResponse<PageResponse<ApiUsageLogResponse>>> myUsage(
                        @AuthenticationPrincipal CompanyApiPrincipal principal,
                        @RequestParam(required = false) Integer page,
                        @RequestParam(required = false) Integer pageSize) {
                return ResponseEntity.ok(ApiResponse.success(
                                usageService.findByCompany(principal.companyId(), page, pageSize),
                                "API usage retrieved successfully"));
        }
}