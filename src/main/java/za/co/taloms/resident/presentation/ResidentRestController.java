package za.co.taloms.resident.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.resident.application.dto.ResidentRequest;
import za.co.taloms.resident.application.dto.ResidentResponse;
import za.co.taloms.resident.application.service.ResidentService;
import za.co.taloms.resident.domain.entity.RelationshipType;
import java.util.List;

@RestController
@RequestMapping("/api/residents")
@RequiredArgsConstructor
public class ResidentRestController {

    private final ResidentService residentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER')")
    public ResponseEntity<ApiResponse<ResidentResponse>> create(
            @Valid @RequestBody ResidentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = residentService.createResident(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Resident created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER')")
    public ResponseEntity<ApiResponse<ResidentResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ResidentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = residentService.updateResident(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Resident updated successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResidentResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(residentService.findAll(), "Residents retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResidentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(residentService.findById(id), "Resident retrieved successfully"));
    }

    @GetMapping("/idnumber/{idNumber}")
    public ResponseEntity<ApiResponse<ResidentResponse>> getByIdNumber(@PathVariable String idNumber) {
        return ResponseEntity.ok(ApiResponse.success(residentService.findByIdNumber(idNumber),
                "Resident retrieved successfully"));
    }

    @GetMapping("/household/{householdId}")
    public ResponseEntity<ApiResponse<List<ResidentResponse>>> getByHousehold(@PathVariable Long householdId) {
        return ResponseEntity.ok(ApiResponse.success(residentService.findByHouseholdId(householdId),
                "Residents retrieved successfully"));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ResidentResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(residentService.findActive(),
                "Active residents retrieved successfully"));
    }

    @GetMapping("/relationship/{relationshipType}")
    public ResponseEntity<ApiResponse<List<ResidentResponse>>> getByRelationship(
            @PathVariable RelationshipType relationshipType) {
        return ResponseEntity.ok(ApiResponse.success(residentService.findByRelationshipType(relationshipType),
                "Residents retrieved successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ResidentResponse>>> search(@RequestParam String name) {
        return ResponseEntity.ok(ApiResponse.success(residentService.searchByName(name),
                "Search completed"));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<ResidentResponse>> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = residentService.deactivateResident(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Resident deactivated successfully"));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<ResidentResponse>> activate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = residentService.activateResident(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Resident activated successfully"));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<ResidentCountDto>> getCounts() {
        var counts = ResidentCountDto.builder()
                .total(residentService.countAll())
                .active(residentService.countActive())
                .build();
        return ResponseEntity.ok(ApiResponse.success(counts, "Counts retrieved successfully"));
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ResidentCountDto {
        private long total;
        private long active;
    }
}