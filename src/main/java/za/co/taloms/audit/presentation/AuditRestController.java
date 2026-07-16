package za.co.taloms.audit.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.audit.application.dto.AuditLogResponse;
import za.co.taloms.audit.application.dto.AuditSearchCriteria;
import za.co.taloms.audit.application.service.AuditService;
import za.co.taloms.audit.domain.entity.AuditAction;
import za.co.taloms.common.ApiResponse;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditRestController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(auditService.findAll(),
                "Audit logs retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(auditService.findById(id),
                "Audit log retrieved successfully"));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {

        return ResponseEntity.ok(ApiResponse.success(
                auditService.findByEntity(entityType, entityId),
                "Audit logs retrieved successfully"));
    }

    @GetMapping("/user/{performedBy}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByUser(
            @PathVariable String performedBy) {

        return ResponseEntity.ok(ApiResponse.success(
                auditService.findByPerformedBy(performedBy),
                "Audit logs retrieved successfully"));
    }

    @GetMapping("/action/{action}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByAction(
            @PathVariable AuditAction action) {

        return ResponseEntity.ok(ApiResponse.success(
                auditService.findByAction(action),
                "Audit logs retrieved successfully"));
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> search(
            @RequestBody AuditSearchCriteria criteria) {

        return ResponseEntity.ok(ApiResponse.success(
                auditService.search(criteria),
                "Audit logs retrieved successfully"));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<AuditCountDto>> getCounts() {
        var counts = AuditCountDto.builder()
                .total(auditService.countAll())
                .build();
        return ResponseEntity.ok(ApiResponse.success(counts, "Counts retrieved successfully"));
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AuditCountDto {
        private long total;
    }
}