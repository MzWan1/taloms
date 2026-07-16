package za.co.taloms.document.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.document.application.dto.DocumentAccessLogResponse;
import za.co.taloms.document.application.dto.DocumentResponse;
import za.co.taloms.document.application.dto.DocumentUploadRequest;
import za.co.taloms.document.application.service.DocumentService;
import za.co.taloms.document.domain.entity.DocumentType;
import za.co.taloms.document.domain.entity.EntityType;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentRestController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER','ROLE_LAND_OFFICER')")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @Valid @RequestPart("request") DocumentUploadRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        var response = documentService.uploadDocument(
                file, request, userDetails.getUsername(), clientIp, userAgent);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Document uploaded successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(documentService.findAll(),
                "Documents retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<DocumentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(documentService.findById(id),
                "Document retrieved successfully"));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getByEntity(
            @PathVariable EntityType entityType,
            @PathVariable Long entityId) {

        return ResponseEntity.ok(ApiResponse.success(
                documentService.findByRelatedEntity(entityType, entityId),
                "Documents retrieved successfully"));
    }

    @GetMapping("/type/{documentType}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getByType(
            @PathVariable DocumentType documentType) {

        return ResponseEntity.ok(ApiResponse.success(
                documentService.findByDocumentType(documentType),
                "Documents retrieved successfully"));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER','ROLE_LAND_OFFICER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<byte[]> download(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        var document = documentService.findById(id);
        byte[] content = documentService.downloadDocument(
                id, userDetails.getUsername(), clientIp, userAgent);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(document.getContentType()));
        headers.setContentDispositionFormData("attachment", document.getOriginalFilename());
        headers.setContentLength(content.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_DATA_CAPTURER')")
    public ResponseEntity<ApiResponse<DocumentResponse>> update(
            @PathVariable Long id,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String notes) {

        var response = documentService.updateDocument(id, description, notes);
        return ResponseEntity.ok(ApiResponse.success(response, "Document updated successfully"));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<DocumentResponse>> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = documentService.deactivateDocument(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Document deactivated successfully"));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<DocumentResponse>> activate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        var response = documentService.activateDocument(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Document activated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        documentService.deleteDocument(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted successfully"));
    }

    @GetMapping("/{id}/access-logs")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<List<DocumentAccessLogResponse>>> getAccessLogs(
            @PathVariable Long id) {

        var response = documentService.getDocumentAccessLogs(id);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Document access logs retrieved successfully"));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<DocumentCountDto>> getCounts() {
        var counts = DocumentCountDto.builder()
                .total(documentService.countAll())
                .build();
        return ResponseEntity.ok(ApiResponse.success(counts, "Counts retrieved successfully"));
    }

    // Inner DTO class
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DocumentCountDto {
        private long total;
    }
}