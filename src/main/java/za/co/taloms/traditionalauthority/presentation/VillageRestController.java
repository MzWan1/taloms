package za.co.taloms.traditionalauthority.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.traditionalauthority.application.dto.*;
import za.co.taloms.traditionalauthority.application.service.VillageService;
import za.co.taloms.security.application.service.AuthorityScopeService;
import java.util.List;

@RestController
@RequestMapping("/api/villages")
@RequiredArgsConstructor
public class VillageRestController {

    private final VillageService villageService;
    private final AuthorityScopeService scopeService;

    /** Chiefs/headsmen only see villages of their linked authority. */
    private List<VillageResponse> scoped(List<VillageResponse> all) {
        if (!scopeService.isCurrentUserChiefOrHeadsman()) {
            return all;
        }
        Long linkedAuthorityId = scopeService.getCurrentUserAuthorityId();
        if (linkedAuthorityId == null) {
            return List.of();
        }
        return all.stream()
                .filter(v -> linkedAuthorityId.equals(v.getTraditionalAuthorityId()))
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('CHIEF')")
    public ResponseEntity<ApiResponse<VillageResponse>> create(
            @Valid @RequestBody VillageRequest request) {
        // Validate CHIEF can only add villages to their linked authority
        scopeService.requireAuthorityAccess(request.getTraditionalAuthorityId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        villageService.create(request),
                        "Village created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CHIEF','HEADSMAN')")
    public ResponseEntity<ApiResponse<List<VillageResponse>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success(scoped(villageService.findAll()),
                        "Villages retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHIEF','HEADSMAN')")
    public ResponseEntity<ApiResponse<VillageResponse>> getById(
            @PathVariable Long id) {
        var village = villageService.findById(id);
        // Chiefs/headsmen may only view villages in their linked authority
        scopeService.requireAuthorityAccess(village.getTraditionalAuthorityId());
        return ResponseEntity.ok(
                ApiResponse.success(village, "Village retrieved successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CHIEF','HEADSMAN')")
    public ResponseEntity<ApiResponse<List<VillageResponse>>> search(
            @RequestParam String name) {
        return ResponseEntity.ok(
                ApiResponse.success(scoped(villageService.searchByName(name)),
                        "Search completed"));
    }

    @GetMapping("/by-authority/{authorityId}")
    @PreAuthorize("hasAnyRole('CHIEF','HEADSMAN')")
    public ResponseEntity<ApiResponse<List<VillageResponse>>>
    getByAuthority(@PathVariable Long authorityId) {
        // Chiefs/headsmen may only view villages of their linked authority
        scopeService.requireAuthorityAccess(authorityId);
        return ResponseEntity.ok(
                ApiResponse.success(
                        villageService.findByAuthority(authorityId),
                        "Villages retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CHIEF')")
    public ResponseEntity<ApiResponse<VillageResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody VillageRequest request) {
        // Validate CHIEF can only update villages in their linked authority
        scopeService.requireAuthorityAccess(request.getTraditionalAuthorityId());
        return ResponseEntity.ok(
                ApiResponse.success(
                        villageService.update(id, request),
                        "Village updated successfully"));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('CHIEF')")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable Long id) {
        var village = villageService.findById(id);
        // Validate CHIEF can only modify villages in their linked authority
        scopeService.requireAuthorityAccess(village.getTraditionalAuthorityId());
        villageService.deactivate(id);
        return ResponseEntity.ok(
                ApiResponse.success(null,
                        "Village deactivated successfully"));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('CHIEF')")
    public ResponseEntity<ApiResponse<Void>> activate(
            @PathVariable Long id) {
        var village = villageService.findById(id);
        // Validate CHIEF can only modify villages in their linked authority
        scopeService.requireAuthorityAccess(village.getTraditionalAuthorityId());
        villageService.activate(id);
        return ResponseEntity.ok(
                ApiResponse.success(null,
                        "Village activated successfully"));
    }
}


