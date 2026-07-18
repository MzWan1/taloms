package za.co.taloms.household.presentation;

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
import za.co.taloms.household.application.dto.HouseholdRequest;
import za.co.taloms.household.application.dto.HouseholdResponse;
import za.co.taloms.household.application.service.HouseholdService;

import java.util.List;

@Slf4j
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

        log.info("REST API: Creating household - Head: {}, Parcel: {}",
                request.getHouseholdHeadName(), request.getParcelId());

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

        log.info("REST API: Updating household - ID: {}, Head: {}", id, request.getHouseholdHeadName());

        var response = householdService.updateHousehold(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Household updated successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<List<HouseholdResponse>>> getAll() {
        log.debug("REST API: Getting all households");
        return ResponseEntity.ok(ApiResponse.success(householdService.findAll(),
                "Households retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<HouseholdResponse>> getById(@PathVariable Long id) {
        log.debug("REST API: Getting household by ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success(householdService.findById(id),
                "Household retrieved successfully"));
    }

    @GetMapping("/parcel/{parcelId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<List<HouseholdResponse>>> getByParcel(@PathVariable Long parcelId) {
        log.debug("REST API: Getting households by parcel: {}", parcelId);
        return ResponseEntity.ok(ApiResponse.success(householdService.findByParcelId(parcelId),
                "Households retrieved successfully"));
    }

    @GetMapping("/parcel/{parcelId}/active")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<HouseholdResponse>> getActiveByParcel(@PathVariable Long parcelId) {
        log.debug("REST API: Getting active household by parcel: {}", parcelId);
        var household = householdService.findActiveByParcelId(parcelId);
        if (household == null) {
            return ResponseEntity.ok(ApiResponse.success(null, "No active household found"));
        }
        return ResponseEntity.ok(ApiResponse.success(household, "Active household retrieved successfully"));
    }

    @GetMapping("/pto/{ptoId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<List<HouseholdResponse>>> getByPto(@PathVariable Long ptoId) {
        log.debug("REST API: Getting households by PTO: {}", ptoId);
        return ResponseEntity.ok(ApiResponse.success(householdService.findByPtoId(ptoId),
                "Households retrieved successfully"));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<List<HouseholdResponse>>> getActive() {
        log.debug("REST API: Getting active households");
        return ResponseEntity.ok(ApiResponse.success(householdService.findActive(),
                "Active households retrieved successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<List<HouseholdResponse>>> search(@RequestParam String name) {
        log.debug("REST API: Searching households by name: {}", name);
        return ResponseEntity.ok(ApiResponse.success(householdService.searchByName(name),
                "Search completed"));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<HouseholdResponse>> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("REST API: Deactivating household - ID: {}", id);
        var response = householdService.deactivateHousehold(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Household deactivated successfully"));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<HouseholdResponse>> activate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("REST API: Activating household - ID: {}", id);
        var response = householdService.activateHousehold(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Household activated successfully"));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<HouseholdCountDto>> getCounts() {
        log.debug("REST API: Getting household counts");
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