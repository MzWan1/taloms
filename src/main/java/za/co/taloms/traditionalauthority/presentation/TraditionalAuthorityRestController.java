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
import za.co.taloms.traditionalauthority.application.dto.*;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import java.util.List;

@RestController
@RequestMapping("/api/authorities")
@RequiredArgsConstructor
public class TraditionalAuthorityRestController {

    private final TraditionalAuthorityService authorityService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
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
                ApiResponse.success(authorityService.findAll(),
                        "Authorities retrieved successfully"));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<TraditionalAuthorityResponse>>>
    getAllActive() {
        return ResponseEntity.ok(
                ApiResponse.success(authorityService.findAllActive(),
                        "Active authorities retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TraditionalAuthorityResponse>>
    getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(authorityService.findById(id),
                        "Authority retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<TraditionalAuthorityResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TraditionalAuthorityRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        authorityService.update(id, request),
                        "Authority updated successfully"));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable Long id) {
        authorityService.deactivate(id);
        return ResponseEntity.ok(
                ApiResponse.success(null,
                        "Authority deactivated successfully"));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activate(
            @PathVariable Long id) {
        authorityService.activate(id);
        return ResponseEntity.ok(
                ApiResponse.success(null,
                        "Authority activated successfully"));
    }
}