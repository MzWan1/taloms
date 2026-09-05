package za.co.taloms.parcel.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.parcel.application.dto.ParcelRequest;
import za.co.taloms.parcel.application.dto.ParcelResponse;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.security.application.service.AuthorityScopeService;
import za.co.taloms.traditionalauthority.application.service.VillageService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/parcels")
@RequiredArgsConstructor
public class ParcelRestController {

    private final ParcelService parcelService;
    private final VillageService villageService;
    private final AuthorityScopeService scopeService;

    /** Returns the village IDs in the current chief/headman's authority, or null if unrestricted. */
    private Set<Long> scopedVillageIds() {
        if (!scopeService.isCurrentUserChiefOrHeadsman()) {
            return null;
        }
        Long linkedAuthorityId = scopeService.getCurrentUserAuthorityId();
        if (linkedAuthorityId == null) {
            return Set.of();
        }
        return villageService.findByAuthority(linkedAuthorityId).stream()
                .map(v -> v.getId())
                .collect(Collectors.toSet());
    }

    private List<ParcelResponse> scoped(List<ParcelResponse> all) {
        Set<Long> allowedVillageIds = scopedVillageIds();
        if (allowedVillageIds == null) {
            return all;
        }
        return all.stream()
                .filter(p -> p.getVillageId() != null
                        && allowedVillageIds.contains(p.getVillageId()))
                .toList();
    }

    /** Throws SecurityException if the parcel is outside the current user's scope. */
    private void requireParcelAccess(ParcelResponse parcel) {
        if (parcel == null) {
            throw new SecurityException("Parcel not found");
        }
        if (parcel.getVillageId() == null) {
            throw new SecurityException("Parcel has no linked village");
        }
        var village = villageService.findById(parcel.getVillageId());
        scopeService.requireAuthorityAccess(village.getTraditionalAuthorityId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF','HEADSMAN')")
    public ResponseEntity<ApiResponse<ParcelResponse>> create(
            @Valid @RequestBody ParcelRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Chiefs/headsmen can only create parcels in their own authority's villages
        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            if (request.getVillageId() == null) {
                throw new SecurityException("A village must be selected.");
            }
            var village = villageService.findById(request.getVillageId());
            scopeService.requireAuthorityAccess(village.getTraditionalAuthorityId());
        }

        var response = parcelService.createParcel(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Parcel created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF','HEADSMAN')")
    public ResponseEntity<ApiResponse<ParcelResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ParcelRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Chiefs/headsmen can only update parcels in their own authority
        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requireParcelAccess(parcelService.findById(id));
        }

        var response = parcelService.updateParcel(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Parcel updated successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ParcelResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                scoped(parcelService.findAll()), "Parcels retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ParcelResponse>> getById(@PathVariable Long id) {
        var parcel = parcelService.findById(id);
        // Chiefs/headsmen may only view parcels in their authority
        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requireParcelAccess(parcel);
        }
        return ResponseEntity.ok(ApiResponse.success(parcel, "Parcel retrieved successfully"));
    }

    @GetMapping("/number/{parcelNumber}")
    public ResponseEntity<ApiResponse<ParcelResponse>> getByNumber(@PathVariable String parcelNumber) {
        var parcel = parcelService.findByParcelNumber(parcelNumber);
        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requireParcelAccess(parcel);
        }
        return ResponseEntity.ok(ApiResponse.success(parcel,
                "Parcel retrieved successfully"));
    }

    @GetMapping("/village/{villageId}")
    public ResponseEntity<ApiResponse<List<ParcelResponse>>> getByVillage(@PathVariable Long villageId) {
        // Chiefs/headsmen may only view parcels of villages in their authority
        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            var village = villageService.findById(villageId);
            scopeService.requireAuthorityAccess(village.getTraditionalAuthorityId());
        }
        return ResponseEntity.ok(ApiResponse.success(parcelService.findByVillage(villageId),
                "Parcels retrieved successfully"));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ParcelResponse>>> getByStatus(@PathVariable ParcelStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                scoped(parcelService.findByStatus(status)),
                "Parcels retrieved successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ParcelResponse>>> search(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(
                scoped(parcelService.search(q)), "Search completed"));
    }

    @GetMapping("/available/{villageId}")
    public ResponseEntity<ApiResponse<List<ParcelResponse>>> getAvailable(@PathVariable Long villageId) {
        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            var village = villageService.findById(villageId);
            scopeService.requireAuthorityAccess(village.getTraditionalAuthorityId());
        }
        return ResponseEntity.ok(ApiResponse.success(parcelService.findAvailable(villageId),
                "Available parcels retrieved successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    public ResponseEntity<ApiResponse<ParcelResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam ParcelStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requireParcelAccess(parcelService.findById(id));
        }

        var response = parcelService.updateStatus(id, status, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Parcel status updated successfully"));
    }

    @PatchMapping("/{id}/allocate")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF','HEADSMAN')")
    public ResponseEntity<ApiResponse<ParcelResponse>> allocate(
            @PathVariable Long id,
            @RequestParam Long ptoId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requireParcelAccess(parcelService.findById(id));
        }

        var response = parcelService.allocateParcel(id, ptoId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Parcel allocated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        parcelService.deleteParcel(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Parcel deleted successfully"));
    }
}


