package za.co.taloms.pto.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.pto.application.dto.*;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.pto.domain.entity.PTOStatus;
import java.util.List;

@RestController
@RequestMapping("/api/ptos")
@RequiredArgsConstructor
public class PTORestController {

    private final PTOService ptoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR'," +
            "'ROLE_LAND_OFFICER','ROLE_DATA_CAPTURER')")
    public ResponseEntity<ApiResponse<PTOResponse>> create(
            @Valid @RequestBody PTORequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = ptoService.createPTO(
                request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response,
                        "PTO created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PTOResponse>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success(ptoService.findAll(),
                        "PTOs retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PTOResponse>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(ptoService.findById(id),
                        "PTO retrieved successfully"));
    }

    @GetMapping("/number/{ptoNumber}")
    public ResponseEntity<ApiResponse<PTOResponse>> getByNumber(
            @PathVariable String ptoNumber) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        ptoService.findByPtoNumber(ptoNumber),
                        "PTO retrieved successfully"));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<PTOResponse>>> getByStatus(
            @PathVariable PTOStatus status) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        ptoService.findByStatus(status),
                        "PTOs retrieved successfully"));
    }

    @GetMapping("/authority/{authorityId}")
    public ResponseEntity<ApiResponse<List<PTOResponse>>> getByAuthority(
            @PathVariable Long authorityId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        ptoService.findByAuthority(authorityId),
                        "PTOs retrieved successfully"));
    }

    @GetMapping("/village/{villageId}")
    public ResponseEntity<ApiResponse<List<PTOResponse>>> getByVillage(
            @PathVariable Long villageId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        ptoService.findByVillage(villageId),
                        "PTOs retrieved successfully"));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<PTOResponse>>> search(
            @RequestBody PTOSearchCriteria criteria) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        ptoService.search(criteria),
                        "Search completed"));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<PTOResponse>> approve(
            @PathVariable Long id,
            @RequestBody(required = false) PTOApprovalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (request == null) request = new PTOApprovalRequest();
        return ResponseEntity.ok(
                ApiResponse.success(
                        ptoService.approvePTO(
                                id, request,
                                userDetails.getUsername()),
                        "PTO approved successfully"));
    }

    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<PTOResponse>> revoke(
            @PathVariable Long id,
            @Valid @RequestBody PTORevokeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        ptoService.revokePTO(
                                id, request,
                                userDetails.getUsername()),
                        "PTO revoked successfully"));
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<PTOResponse>> suspend(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        ptoService.suspendPTO(
                                id, userDetails.getUsername()),
                        "PTO suspended successfully"));
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<PTOResponse>> reactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        ptoService.reactivatePTO(
                                id, userDetails.getUsername()),
                        "PTO reactivated successfully"));
    }
}