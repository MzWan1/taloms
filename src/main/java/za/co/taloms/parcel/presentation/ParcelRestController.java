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
import za.co.taloms.parcel.domain.entity.ParcelType;
import java.util.List;

@RestController
@RequestMapping("/api/parcels")
@RequiredArgsConstructor
public class ParcelRestController {

    private final ParcelService parcelService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_LAND_OFFICER')")
    public ResponseEntity<ApiResponse<ParcelResponse>> create(
            @Valid @RequestBody ParcelRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = parcelService.createParcel(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Parcel created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_LAND_OFFICER')")
    public ResponseEntity<ApiResponse<ParcelResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ParcelRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = parcelService.updateParcel(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Parcel updated successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ParcelResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(parcelService.findAll(), "Parcels retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ParcelResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(parcelService.findById(id), "Parcel retrieved successfully"));
    }

    @GetMapping("/number/{parcelNumber}")
    public ResponseEntity<ApiResponse<ParcelResponse>> getByNumber(@PathVariable String parcelNumber) {
        return ResponseEntity.ok(ApiResponse.success(parcelService.findByParcelNumber(parcelNumber),
                "Parcel retrieved successfully"));
    }

    @GetMapping("/village/{villageId}")
    public ResponseEntity<ApiResponse<List<ParcelResponse>>> getByVillage(@PathVariable Long villageId) {
        return ResponseEntity.ok(ApiResponse.success(parcelService.findByVillage(villageId),
                "Parcels retrieved successfully"));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ParcelResponse>>> getByStatus(@PathVariable ParcelStatus status) {
        return ResponseEntity.ok(ApiResponse.success(parcelService.findByStatus(status),
                "Parcels retrieved successfully"));
    }

    @GetMapping("/type/{parcelType}")
    public ResponseEntity<ApiResponse<List<ParcelResponse>>> getByType(@PathVariable ParcelType parcelType) {
        return ResponseEntity.ok(ApiResponse.success(parcelService.findByParcelType(parcelType),
                "Parcels retrieved successfully"));
    }

    @GetMapping("/available/{villageId}")
    public ResponseEntity<ApiResponse<List<ParcelResponse>>> getAvailable(@PathVariable Long villageId) {
        return ResponseEntity.ok(ApiResponse.success(parcelService.findAvailable(villageId),
                "Available parcels retrieved successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<ParcelResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam ParcelStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = parcelService.updateStatus(id, status, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Parcel status updated successfully"));
    }

    @PatchMapping("/{id}/allocate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_LAND_OFFICER')")
    public ResponseEntity<ApiResponse<ParcelResponse>> allocate(
            @PathVariable Long id,
            @RequestParam Long ptoId,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = parcelService.allocateParcel(id, ptoId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Parcel allocated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        parcelService.deleteParcel(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Parcel deleted successfully"));
    }
}