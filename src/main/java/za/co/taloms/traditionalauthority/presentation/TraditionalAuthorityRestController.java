package za.co.taloms.traditionalauthority.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.security.application.service.AuthorityScopeService;
import za.co.taloms.traditionalauthority.application.dto.*;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import java.util.List;

@RestController
@RequestMapping("/api/authorities")
@RequiredArgsConstructor
public class TraditionalAuthorityRestController {

    private final TraditionalAuthorityService authorityService;
    private final AuthorityScopeService       scopeService;

    /** Chiefs/headsmen only see their own authorities; admins see all. */
    private List<TraditionalAuthorityResponse> scoped(List<TraditionalAuthorityResponse> all) {
        if (!scopeService.isCurrentUserChiefOrHeadsman()) {
            return all;
        }
        java.util.Set<Long> allowed = scopeService.getCurrentUserAuthorityIds();
        if (allowed == null || allowed.isEmpty()) {
            return List.of();
        }
        return all.stream()
                .filter(a -> allowed.contains(a.getId()))
                .toList();
    }

    /** Chiefs linked to the given authority (ADMIN view of the relationship). */
    @GetMapping("/{id}/chiefs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TraditionalAuthorityChiefDto>>> getChiefs(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                authorityService.findChiefs(id),
                "Chiefs retrieved successfully"));
    }

    /** Link a chief to the authority. ADMIN only. */
    @PostMapping("/{id}/chiefs/{chiefId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TraditionalAuthorityChiefDto>>> addChief(
            @PathVariable Long id,
            @PathVariable Long chiefId) {
        authorityService.addChiefToAuthority(id, chiefId);
        return ResponseEntity.ok(ApiResponse.success(
                authorityService.findChiefs(id),
                "Chief linked to authority successfully"));
    }

    /** Unlink a chief from the authority. ADMIN only. */
    @DeleteMapping("/{id}/chiefs/{chiefId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TraditionalAuthorityChiefDto>>> removeChief(
            @PathVariable Long id,
            @PathVariable Long chiefId) {
        authorityService.removeChiefFromAuthority(id, chiefId);
        return ResponseEntity.ok(ApiResponse.success(
                authorityService.findChiefs(id),
                "Chief removed from authority successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TraditionalAuthorityResponse>> create(
            @Valid @RequestBody TraditionalAuthorityRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = authorityService.create(
                request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response,
                        "Traditional Authority created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TraditionalAuthorityResponse>>>
    getAll() {
        return ResponseEntity.ok(
                ApiResponse.success(scoped(authorityService.findAll()),
                        "Authorities retrieved successfully"));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<TraditionalAuthorityResponse>>>
    getAllActive() {
        return ResponseEntity.ok(
                ApiResponse.success(scoped(authorityService.findAllActive()),
                        "Active authorities retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TraditionalAuthorityResponse>>
    getById(@PathVariable Long id) {
        // Chiefs/headsmen may only view their own authority
        scopeService.requireAuthorityAccess(id);
        return ResponseEntity.ok(
                ApiResponse.success(authorityService.findById(id),
                        "Authority retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TraditionalAuthorityResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TraditionalAuthorityRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        authorityService.update(id, request),
                        "Authority updated successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TraditionalAuthorityResponse>>> search(
            @RequestParam String name) {
        return ResponseEntity.ok(
                ApiResponse.success(scoped(authorityService.searchByName(name)),
                        "Search completed"));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable Long id) {
        authorityService.deactivate(id);
        return ResponseEntity.ok(
                ApiResponse.success(null,
                        "Authority deactivated successfully"));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activate(
            @PathVariable Long id) {
        authorityService.activate(id);
        return ResponseEntity.ok(
                ApiResponse.success(null,
                        "Authority activated successfully"));
    }
}


