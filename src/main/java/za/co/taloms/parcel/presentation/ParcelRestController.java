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
import za.co.taloms.parcel.application.dto.ParcelSyncDto;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.security.application.service.AuthorityScopeService;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/parcels")
@RequiredArgsConstructor
public class ParcelRestController {

    private final ParcelService parcelService;
    private final AuthorityScopeService scopeService;

    /** Returns the village IDs the current chief/headman may manage, or null if unrestricted (admin). */
    private Set<Long> scopedVillageIds() {
        return scopeService.scopedVillageIds();
    }

    /** Throws SecurityException if the village is outside the current user's scope. */
    private void requireVillageAccess(Long villageId) {
        if (villageId == null) {
            throw new SecurityException("A village must be selected.");
        }
        scopeService.requireVillageAccess(villageId);
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
        requireVillageAccess(parcel.getVillageId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF','HEADSMAN')")
    public ResponseEntity<ApiResponse<ParcelResponse>> create(
            @Valid @RequestBody ParcelRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requireVillageAccess(request.getVillageId());
        }

        var response = parcelService.createParcel(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(String.valueOf(response.getVersion()))
                .body(ApiResponse.success(response, "Parcel created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF','HEADSMAN')")
    public ResponseEntity<ApiResponse<ParcelResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ParcelRequest request,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requireParcelAccess(parcelService.findById(id));
        }

        if (ifMatch != null) {
            try {
                Long clientVersion = Long.parseLong(ifMatch);
                var current = parcelService.findById(id);
                if (!clientVersion.equals(current.getVersion())) {
                    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                            .eTag(String.valueOf(current.getVersion()))
                            .body(ApiResponse.error("Parcel has been modified by another user. Current version: " + current.getVersion()));
                }
            } catch (NumberFormatException e) {
                // Invalid version format, ignore
            }
        }

        var response = parcelService.updateParcel(id, request, userDetails.getUsername());
        return ResponseEntity.ok()
                .eTag(String.valueOf(response.getVersion()))
                .body(ApiResponse.success(response, "Parcel updated successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ParcelResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                scoped(parcelService.findAll()), "Parcels retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ParcelResponse>> getById(@PathVariable Long id) {
        var parcel = parcelService.findById(id);
        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requireParcelAccess(parcel);
        }
        return ResponseEntity.ok()
                .eTag(String.valueOf(parcel.getVersion()))
                .body(ApiResponse.success(parcel, "Parcel retrieved successfully"));
    }

    @GetMapping("/number/{parcelNumber}")
    public ResponseEntity<ApiResponse<ParcelResponse>> getByNumber(@PathVariable String parcelNumber) {
        var parcel = parcelService.findByParcelNumber(parcelNumber);
        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requireParcelAccess(parcel);
        }
        return ResponseEntity.ok()
                .eTag(String.valueOf(parcel.getVersion()))
                .body(ApiResponse.success(parcel, "Parcel retrieved successfully"));
    }

    @GetMapping("/village/{villageId}")
    public ResponseEntity<ApiResponse<List<ParcelResponse>>> getByVillage(@PathVariable Long villageId) {
        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requireVillageAccess(villageId);
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
            requireVillageAccess(villageId);
        }
        return ResponseEntity.ok(ApiResponse.success(parcelService.findAvailable(villageId),
                "Available parcels retrieved successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    public ResponseEntity<ApiResponse<ParcelResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam ParcelStatus status,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requireParcelAccess(parcelService.findById(id));
        }

        if (ifMatch != null) {
            try {
                Long clientVersion = Long.parseLong(ifMatch);
                var current = parcelService.findById(id);
                if (!clientVersion.equals(current.getVersion())) {
                    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                            .eTag(String.valueOf(current.getVersion()))
                            .body(ApiResponse.error("Parcel has been modified by another user. Current version: " + current.getVersion()));
                }
            } catch (NumberFormatException e) {
                // Invalid version format, ignore
            }
        }

        var response = parcelService.updateStatus(id, status, userDetails.getUsername());
        return ResponseEntity.ok()
                .eTag(String.valueOf(response.getVersion()))
                .body(ApiResponse.success(response, "Parcel status updated successfully"));
    }

    @PatchMapping("/{id}/allocate")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF','HEADSMAN')")
    public ResponseEntity<ApiResponse<ParcelResponse>> allocate(
            @PathVariable Long id,
            @RequestParam Long ptoId,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            requireParcelAccess(parcelService.findById(id));
        }

        if (ifMatch != null) {
            try {
                Long clientVersion = Long.parseLong(ifMatch);
                var current = parcelService.findById(id);
                if (!clientVersion.equals(current.getVersion())) {
                    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                            .eTag(String.valueOf(current.getVersion()))
                            .body(ApiResponse.error("Parcel has been modified by another user. Current version: " + current.getVersion()));
                }
            } catch (NumberFormatException e) {
                // Invalid version format, ignore
            }
        }

        var response = parcelService.allocateParcel(id, ptoId, userDetails.getUsername());
        return ResponseEntity.ok()
                .eTag(String.valueOf(response.getVersion()))
                .body(ApiResponse.success(response, "Parcel allocated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (ifMatch != null) {
            try {
                Long clientVersion = Long.parseLong(ifMatch);
                var current = parcelService.findById(id);
                if (!clientVersion.equals(current.getVersion())) {
                    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                            .eTag(String.valueOf(current.getVersion()))
                            .body(ApiResponse.error("Parcel has been modified by another user. Current version: " + current.getVersion()));
                }
            } catch (NumberFormatException e) {
                // Invalid version format, ignore
            }
        }

        parcelService.deleteParcel(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Parcel deleted successfully"));
    }

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.success("pong", "TALOMS is reachable"));
    }

    // Sync endpoints
    @GetMapping("/sync/delta")
    public ResponseEntity<ApiResponse<SyncResponse>> getDelta(
            @RequestParam(required = false) String lastSyncAt,
            @RequestParam(defaultValue = "100") int pageSize) {

        Instant since = null;
        if (lastSyncAt != null && !lastSyncAt.isBlank()) {
            try {
                since = Instant.parse(lastSyncAt);
            } catch (Exception e) {
                // ignore invalid timestamp
            }
        }

        List<ParcelSyncDto> changes = parcelService.findChangedSince(since, pageSize);
        Instant serverTime = Instant.now();
        
        return ResponseEntity.ok(ApiResponse.success(
                new SyncResponse(changes, serverTime), "Delta sync completed"));
    }

    @PostMapping("/sync/push")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF','HEADSMAN')")
    public ResponseEntity<ApiResponse<SyncResult>> pushChanges(
            @Valid @RequestBody SyncPushRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            parcelService.saveAll(request.changes(), userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.success(
                    new SyncResult(true, request.changes().size(), 0), "Push sync completed"));
        } catch (za.co.taloms.common.BusinessValidationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF','HEADSMAN')")
    public ResponseEntity<ApiResponse<List<ParcelResponse>>> createBatch(
            @Valid @RequestBody List<ParcelRequest> requests,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (scopeService.isCurrentUserChiefOrHeadsman()) {
            for (ParcelRequest request : requests) {
                requireVillageAccess(request.getVillageId());
            }
        }

        List<ParcelResponse> responses = parcelService.createBatch(requests, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(responses, "Batch create completed"));
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBatch(
            @RequestBody Set<Long> ids,
            @AuthenticationPrincipal UserDetails userDetails) {

        parcelService.deleteBatch(ids, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Batch delete completed"));
    }

    // Sync DTOs
    public record SyncResponse(List<ParcelSyncDto> data, Instant serverTime) {}
    public record SyncPushRequest(List<ParcelSyncDto> changes) {}
    public record SyncResult(boolean success, int processed, int failed) {}
}


