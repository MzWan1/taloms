package za.co.taloms.household.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.household.application.dto.HouseholdRequest;
import za.co.taloms.household.application.dto.HouseholdResponse;
import za.co.taloms.household.application.service.HouseholdService;
import java.util.List;

@RestController
@RequestMapping("/api/households")
@RequiredArgsConstructor
public class HouseholdRestController {

    private final HouseholdService householdService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER')")
    public ResponseEntity<ApiResponse<HouseholdResponse>> create(
            @Valid @RequestBody HouseholdRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = householdService.createHousehold(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Household created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER')")
    public ResponseEntity<ApiResponse<HouseholdResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody HouseholdRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = householdService.updateHousehold(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Household updated successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HouseholdResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(householdService.findAll(), "Households retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HouseholdResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(householdService.findById(id), "Household retrieved successfully"));
    }

    @GetMapping("/parcel/{parcelId}")
    public ResponseEntity<ApiResponse<List<HouseholdResponse>>> getByParcel(@PathVariable Long parcelId) {
        return ResponseEntity.ok(ApiResponse.success(householdService.findByParcelId(parcelId),
                "Households retrieved successfully"));
    }

    @GetMapping("/parcel/{parcelId}/active")
    public ResponseEntity<ApiResponse<HouseholdResponse>> getActiveByParcel(@PathVariable Long parcelId) {
        var household = householdService.findActiveByParcelId(parcelId);
        if (household == null) {
            return ResponseEntity.ok(ApiResponse.success(null, "No active household found"));
        }
        return ResponseEntity.ok(ApiResponse.success(household, "Active household retrieved successfully"));
    }

    @GetMapping("/pto/{ptoId}")
    public ResponseEntity<ApiResponse<List<HouseholdResponse>>> getByPto(@PathVariable Long ptoId) {
        return ResponseEntity.ok(ApiResponse.success(householdService.findByPtoId(ptoId),
                "Households retrieved successfully"));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<HouseholdResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(householdService.findActive(),
                "Active households retrieved successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<HouseholdResponse>>> search(@RequestParam String name) {
        return ResponseEntity.ok(ApiResponse.success(householdService.searchByName(name),
                "Search completed"));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<HouseholdResponse>> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = householdService.deactivateHousehold(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Household deactivated successfully"));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<HouseholdResponse>> activate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = householdService.activateHousehold(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Household activated successfully"));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<HouseholdCountDto>> getCounts() {
        var counts = HouseholdCountDto.builder()
                .total(householdService.countAll())
                .active(householdService.countActive())
                .build();
        return ResponseEntity.ok(ApiResponse.success(counts, "Counts retrieved successfully"));
    }

    // Inner DTO class
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HouseholdCountDto {
        private long total;
        private long active;
    }
}