package za.co.taloms.pto.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.pto.application.dto.*;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.pto.application.service.PTOCertificatePdfGenerator;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.security.application.service.AuthorityScopeService;
import java.util.List;

@RestController
@RequestMapping("/api/ptos")
@RequiredArgsConstructor
public class PTORestController {

    private final PTOService ptoService;
    private final PTOCertificatePdfGenerator ptoCertificatePdfGenerator;
    private final AuthorityScopeService scopeService;

    /** Throws SecurityException if the PTO's authority is outside the current user's scope. */
    private void requirePtoAccess(Long ptoId) {
        var pto = ptoService.findById(ptoId);
        scopeService.requireAuthorityAccess(pto.getTraditionalAuthorityId());
    }

    private List<PTOResponse> scoped(List<PTOResponse> all) {
        if (!scopeService.isCurrentUserChiefOrHeadsman()) {
            return all;
        }
        Long linkedAuthorityId = scopeService.getCurrentUserAuthorityId();
        if (linkedAuthorityId == null) {
            return List.of();
        }
        return all.stream()
                .filter(p -> linkedAuthorityId.equals(p.getTraditionalAuthorityId()))
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF','HEADSMAN')")
    public ResponseEntity<ApiResponse<PTOResponse>> create(
            @Valid @RequestBody PTORequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Chiefs/headsmen can only create PTOs for their own authority
        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            scopeService.requireAuthorityAccess(request.getTraditionalAuthorityId());
        }

        var response = ptoService.createPTO(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "PTO created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PTOResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                scoped(ptoService.findAll()), "PTOs retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PTOResponse>> getById(@PathVariable Long id) {
        // Chiefs/headsmen may only view PTOs of their own authority
        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requirePtoAccess(id);
        }
        return ResponseEntity.ok(ApiResponse.success(ptoService.findById(id), "PTO retrieved successfully"));
    }

    @GetMapping("/number/{ptoNumber}")
    public ResponseEntity<ApiResponse<PTOResponse>> getByNumber(@PathVariable String ptoNumber) {
        var pto = ptoService.findByPtoNumber(ptoNumber);
        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            scopeService.requireAuthorityAccess(pto.getTraditionalAuthorityId());
        }
        return ResponseEntity.ok(ApiResponse.success(pto, "PTO retrieved successfully"));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<PTOResponse>>> getByStatus(@PathVariable PTOStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                scoped(ptoService.findByStatus(status)), "PTOs retrieved successfully"));
    }

    @GetMapping("/authority/{authorityId}")
    public ResponseEntity<ApiResponse<List<PTOResponse>>> getByAuthority(@PathVariable Long authorityId) {
        // Chiefs/headsmen may only view PTOs of their own authority
        scopeService.requireAuthorityAccess(authorityId);
        return ResponseEntity.ok(ApiResponse.success(ptoService.findByAuthority(authorityId), "PTOs retrieved successfully"));
    }

    @GetMapping("/village/{villageId}")
    public ResponseEntity<ApiResponse<List<PTOResponse>>> getByVillage(@PathVariable Long villageId) {
        return ResponseEntity.ok(ApiResponse.success(
                scoped(ptoService.findByVillage(villageId)), "PTOs retrieved successfully"));
    }

    @GetMapping("/parcel/{parcelId}")
    public ResponseEntity<ApiResponse<List<PTOResponse>>> getByParcel(@PathVariable Long parcelId) {
        return ResponseEntity.ok(ApiResponse.success(
                scoped(ptoService.findByParcel(parcelId)), "PTOs retrieved successfully"));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<PTOResponse>>> search(@RequestBody PTOSearchCriteria criteria) {
        // Chiefs/headsmen are pinned to their own authority
        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            Long linkedAuthorityId = scopeService.getCurrentUserAuthorityId();
            if (linkedAuthorityId == null) {
                return ResponseEntity.ok(ApiResponse.success(List.of(), "Search completed"));
            }
            criteria.setAuthorityId(linkedAuthorityId);
        }
        List<PTOResponse> results = ptoService.search(criteria);
        return ResponseEntity.ok(ApiResponse.success(
                scoped(results), "Search completed"));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('CHIEF')")
    public ResponseEntity<ApiResponse<PTOResponse>> approve(
            @PathVariable Long id,
            @RequestBody(required = false) PTOApprovalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Chiefs may only approve PTOs of their own authority
        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requirePtoAccess(id);
        }

        if (request == null) request = new PTOApprovalRequest();
        return ResponseEntity.ok(ApiResponse.success(
                ptoService.approvePTO(id, request, userDetails.getUsername()),
                "PTO approved successfully"));
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    public ResponseEntity<ApiResponse<PTOResponse>> suspend(
            @PathVariable Long id,
            @RequestBody(required = false) PTORevokeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requirePtoAccess(id);
        }

        String reason = (request != null && request.getReason() != null)
                ? request.getReason()
                : "Suspended by system administrator";

        return ResponseEntity.ok(ApiResponse.success(
                ptoService.suspendPTO(id, reason, userDetails.getUsername()),
                "PTO suspended successfully"));
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    public ResponseEntity<ApiResponse<PTOResponse>> reactivate(
            @PathVariable Long id,
            @RequestBody(required = false) PTOApprovalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requirePtoAccess(id);
        }

        String notes = (request != null && request.getNotes() != null)
                ? request.getNotes()
                : null;

        return ResponseEntity.ok(ApiResponse.success(
                ptoService.reactivatePTO(id, notes, userDetails.getUsername()),
                "PTO reactivated successfully"));
    }

    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    public ResponseEntity<ApiResponse<PTOResponse>> revoke(
            @PathVariable Long id,
            @Valid @RequestBody PTORevokeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requirePtoAccess(id);
        }

        return ResponseEntity.ok(ApiResponse.success(
                ptoService.revokePTO(id, request, userDetails.getUsername()),
                "PTO revoked successfully"));
    }

    @PatchMapping("/{id}/reinstate")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    public ResponseEntity<ApiResponse<PTOResponse>> reinstate(
            @PathVariable Long id,
            @RequestBody PTORevokeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requirePtoAccess(id);
        }

        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new IllegalArgumentException("Reinstatement reason is required");
        }

        ptoService.reinstate(id, request.getReason());

        // Return the updated PTO
        var response = ptoService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "PTO reinstated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requirePtoAccess(id);
        }

        ptoService.deletePTO(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "PTO deleted successfully"));
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    public ResponseEntity<ApiResponse<List<PTOResponse>>> getDeleted() {
        return ResponseEntity.ok(ApiResponse.success(
                scoped(ptoService.findDeleted()), "Deleted PTOs retrieved successfully"));
    }

    @GetMapping("/{id}/certificate")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF','HEADSMAN')")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable Long id) {
        // Chiefs/headsmen may only download certificates for their own authority's PTOs
        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requirePtoAccess(id);
        }

        byte[] pdf = ptoCertificatePdfGenerator.generateCertificate(id);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"PTO_" + id + "_Certificate.pdf\"")
                .body(pdf);
    }
}


