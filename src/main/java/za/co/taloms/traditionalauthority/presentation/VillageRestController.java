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
import java.util.List;

@RestController
@RequestMapping("/api/villages")
@RequiredArgsConstructor
public class VillageRestController {

    private final VillageService villageService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<VillageResponse>> create(
            @Valid @RequestBody VillageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        villageService.create(request),
                        "Village created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VillageResponse>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success(villageService.findAll(),
                        "Villages retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VillageResponse>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(villageService.findById(id),
                        "Village retrieved successfully"));
    }

    @GetMapping("/by-authority/{authorityId}")
    public ResponseEntity<ApiResponse<List<VillageResponse>>>
    getByAuthority(@PathVariable Long authorityId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        villageService.findByAuthority(authorityId),
                        "Villages retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<VillageResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody VillageRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        villageService.update(id, request),
                        "Village updated successfully"));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable Long id) {
        villageService.deactivate(id);
        return ResponseEntity.ok(
                ApiResponse.success(null,
                        "Village deactivated successfully"));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<Void>> activate(
            @PathVariable Long id) {
        villageService.activate(id);
        return ResponseEntity.ok(
                ApiResponse.success(null,
                        "Village activated successfully"));
    }
}