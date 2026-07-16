package za.co.taloms.businessoccupancy.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.businessoccupancy.application.dto.BusinessOccupancyRequest;
import za.co.taloms.businessoccupancy.application.dto.BusinessOccupancyResponse;
import za.co.taloms.businessoccupancy.application.service.BusinessOccupancyService;
import za.co.taloms.businessoccupancy.domain.entity.BusinessStatus;
import za.co.taloms.businessoccupancy.domain.entity.BusinessType;
import java.util.List;

@RestController
@RequestMapping("/api/business-occupancies")
@RequiredArgsConstructor
public class BusinessOccupancyRestController {

    private final BusinessOccupancyService businessService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER')")
    public ResponseEntity<ApiResponse<BusinessOccupancyResponse>> create(
            @Valid @RequestBody BusinessOccupancyRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = businessService.createOccupancy(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Business occupancy created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER')")
    public ResponseEntity<ApiResponse<BusinessOccupancyResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody BusinessOccupancyRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = businessService.updateOccupancy(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Business occupancy updated successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusinessOccupancyResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(businessService.findAll(),
                "Business occupancies retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessOccupancyResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(businessService.findById(id),
                "Business occupancy retrieved successfully"));
    }

    @GetMapping("/registration/{registrationNumber}")
    public ResponseEntity<ApiResponse<BusinessOccupancyResponse>> getByRegistrationNumber(
            @PathVariable String registrationNumber) {
        return ResponseEntity.ok(ApiResponse.success(
                businessService.findByRegistrationNumber(registrationNumber),
                "Business occupancy retrieved successfully"));
    }

    @GetMapping("/parcel/{parcelId}")
    public ResponseEntity<ApiResponse<List<BusinessOccupancyResponse>>> getByParcel(@PathVariable Long parcelId) {
        return ResponseEntity.ok(ApiResponse.success(businessService.findByParcelId(parcelId),
                "Business occupancies retrieved successfully"));
    }

    @GetMapping("/pto/{ptoId}")
    public ResponseEntity<ApiResponse<List<BusinessOccupancyResponse>>> getByPto(@PathVariable Long ptoId) {
        return ResponseEntity.ok(ApiResponse.success(businessService.findByPtoId(ptoId),
                "Business occupancies retrieved successfully"));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<BusinessOccupancyResponse>>> getByStatus(
            @PathVariable BusinessStatus status) {
        return ResponseEntity.ok(ApiResponse.success(businessService.findByStatus(status),
                "Business occupancies retrieved successfully"));
    }

    @GetMapping("/type/{businessType}")
    public ResponseEntity<ApiResponse<List<BusinessOccupancyResponse>>> getByType(
            @PathVariable BusinessType businessType) {
        return ResponseEntity.ok(ApiResponse.success(businessService.findByBusinessType(businessType),
                "Business occupancies retrieved successfully"));
    }

    @GetMapping("/search/name")
    public ResponseEntity<ApiResponse<List<BusinessOccupancyResponse>>> searchByName(
            @RequestParam String name) {
        return ResponseEntity.ok(ApiResponse.success(businessService.searchByName(name),
                "Search completed"));
    }

    @GetMapping("/search/owner")
    public ResponseEntity<ApiResponse<List<BusinessOccupancyResponse>>> searchByOwner(
            @RequestParam String ownerName) {
        return ResponseEntity.ok(ApiResponse.success(businessService.searchByOwnerName(ownerName),
                "Search completed"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<BusinessOccupancyResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam BusinessStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = businessService.updateStatus(id, status, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Business status updated successfully"));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<BusinessOccupancyResponse>> activate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = businessService.activateOccupancy(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Business activated successfully"));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<BusinessOccupancyResponse>> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = businessService.deactivateOccupancy(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Business deactivated successfully"));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<BusinessCountDto>> getCounts() {
        var counts = BusinessCountDto.builder()
                .total(businessService.countAll())
                .active(businessService.countByStatus(BusinessStatus.ACTIVE))
                .pending(businessService.countByStatus(BusinessStatus.PENDING))
                .suspended(businessService.countByStatus(BusinessStatus.SUSPENDED))
                .inactive(businessService.countByStatus(BusinessStatus.INACTIVE))
                .build();
        return ResponseEntity.ok(ApiResponse.success(counts, "Counts retrieved successfully"));
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BusinessCountDto {
        private long total;
        private long active;
        private long pending;
        private long suspended;
        private long inactive;
    }
}