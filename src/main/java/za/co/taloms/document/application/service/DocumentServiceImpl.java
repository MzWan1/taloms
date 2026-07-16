package za.co.taloms.document.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.document.application.dto.DocumentAccessLogResponse;
import za.co.taloms.document.application.dto.DocumentResponse;
import za.co.taloms.document.application.dto.DocumentUploadRequest;
import za.co.taloms.document.domain.entity.Document;
import za.co.taloms.document.domain.entity.DocumentAccessLog;
import za.co.taloms.document.domain.entity.DocumentType;
import za.co.taloms.document.domain.entity.EntityType;
import za.co.taloms.document.domain.repository.DocumentAccessLogRepositoryPort;
import za.co.taloms.document.domain.repository.DocumentRepositoryPort;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepositoryPort documentRepository;
    private final DocumentAccessLogRepositoryPort accessLogRepository;

    @Value("${taloms.upload.path:uploads}")
    private String uploadPath;

    @Value("${taloms.upload.max-size:20971520}")
    private long maxFileSize;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/jpg",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );

    @Override
    public DocumentResponse uploadDocument(MultipartFile file, DocumentUploadRequest request,
                                           String uploadedBy, String clientIp, String userAgent) {
        // Validate file
        validateFile(file);

        // Validate entity type
        var entityType = EntityType.valueOf(request.getEntityType());
        var documentType = DocumentType.valueOf(request.getDocumentType());

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + "." + extension;

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(this.uploadPath);
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", e.getMessage(), e);
            throw new BusinessValidationException("Failed to create upload directory");
        }

        // Save file to disk
        Path filePath = uploadPath.resolve(storedFilename);
        try {
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            log.error("Failed to save file: {}", e.getMessage(), e);
            throw new BusinessValidationException("Failed to save file");
        }

        // Calculate checksum
        String checksum = calculateChecksum(filePath);

        // Create document entity
        var document = Document.builder()
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .documentType(documentType)
                .relatedEntityType(entityType)
                .relatedEntityId(request.getEntityId())
                .description(request.getDescription())
                .uploadedBy(uploadedBy)
                .active(true)
                .version(1)
                .checksum(checksum)
                .notes(request.getNotes())
                .build();

        var saved = documentRepository.save(document);
        log.info("Document uploaded: {} ({}) by {} for {}#{}",
                saved.getOriginalFilename(), saved.getStoredFilename(),
                uploadedBy, entityType, request.getEntityId());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse findById(Long id) {
        return documentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse findByStoredFilename(String storedFilename) {
        return documentRepository.findByStoredFilename(storedFilename)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Document with filename: " + storedFilename));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> findAll() {
        return documentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> findByRelatedEntity(EntityType entityType, Long entityId) {
        return documentRepository.findByRelatedEntity(entityType, entityId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> findByDocumentType(DocumentType documentType) {
        return documentRepository.findByDocumentType(documentType).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> findByUploadedBy(String uploadedBy) {
        return documentRepository.findByUploadedBy(uploadedBy).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> findByActive() {
        return documentRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentResponse updateDocument(Long id, String description, String notes) {
        var document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));

        document.setDescription(description);
        document.setNotes(notes);

        var saved = documentRepository.save(document);
        log.info("Document {} updated", saved.getId());

        return toResponse(saved);
    }

    @Override
    public DocumentResponse deactivateDocument(Long id, String deactivatedBy) {
        var document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));

        document.setActive(false);
        var saved = documentRepository.save(document);
        log.info("Document {} deactivated by {}", saved.getId(), deactivatedBy);

        return toResponse(saved);
    }

    @Override
    public DocumentResponse activateDocument(Long id, String activatedBy) {
        var document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));

        document.setActive(true);
        var saved = documentRepository.save(document);
        log.info("Document {} activated by {}", saved.getId(), activatedBy);

        return toResponse(saved);
    }

    @Override
    public void deleteDocument(Long id, String deletedBy) {
        var document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));

        // Delete file from disk
        Path filePath = Paths.get(uploadPath, document.getStoredFilename());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", e.getMessage(), e);
            // Continue with deleting the record even if file deletion fails
        }

        documentRepository.delete(document);
        log.info("Document {} deleted by {}", document.getId(), deletedBy);
    }

    @Override
    public byte[] downloadDocument(Long id, String accessedBy, String clientIp, String userAgent) {
        var document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));

        if (!document.isActive()) {
            throw new BusinessValidationException("Document is not active");
        }

        // Log access
        var accessLog = DocumentAccessLog.builder()
                .documentId(document.getId())
                .accessedBy(accessedBy)
                .accessType("DOWNLOAD")
                .accessIp(clientIp)
                .userAgent(userAgent)
                .build();
        accessLogRepository.save(accessLog);

        // Read file
        Path filePath = Paths.get(uploadPath, document.getStoredFilename());
        try {
            byte[] content = Files.readAllBytes(filePath);
            log.info("Document {} downloaded by {}", document.getId(), accessedBy);
            return content;
        } catch (IOException e) {
            log.error("Failed to read file: {}", e.getMessage(), e);
            throw new BusinessValidationException("Failed to read file");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentAccessLogResponse> getDocumentAccessLogs(Long documentId) {
        // Verify document exists
        if (!documentRepository.findById(documentId).isPresent()) {
            throw new ResourceNotFoundException("Document", documentId);
        }

        return accessLogRepository.findByDocumentIdOrderByAccessedAtDesc(documentId).stream()
                .map(this::toAccessLogResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRelatedEntity(EntityType entityType, Long entityId) {
        return documentRepository.countByRelatedEntity(entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return documentRepository.countAll();
    }

    private void validateFile(MultipartFile file) {
        // Check file size
        if (file.getSize() > maxFileSize) {
            throw new BusinessValidationException(
                    "File size exceeds the maximum allowed size of " + (maxFileSize / (1024 * 1024)) + "MB");
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessValidationException(
                    "File type not supported. Allowed types: PDF, JPEG, PNG, DOC, DOCX");
        }

        // Check if file is empty
        if (file.isEmpty()) {
            throw new BusinessValidationException("File is empty");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private String calculateChecksum(Path filePath) {
        try {
            byte[] content = Files.readAllBytes(filePath);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Failed to calculate checksum: {}", e.getMessage(), e);
            return null;
        }
    }

    private DocumentResponse toResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .originalFilename(document.getOriginalFilename())
                .storedFilename(document.getStoredFilename())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .fileSizeDisplay(document.getFileSizeDisplay())
                .documentType(document.getDocumentType())
                .documentTypeDisplay(document.getDocumentType().getDisplayName())
                .documentTypeBadgeClass(document.getDocumentType().getBadgeClass())
                .documentTypeIcon(document.getDocumentType().getIcon())
                .relatedEntityType(document.getRelatedEntityType())
                .entityTypeDisplay(document.getRelatedEntityType().getDisplayName())
                .relatedEntityId(document.getRelatedEntityId())
                .description(document.getDescription())
                .uploadedBy(document.getUploadedBy())
                .uploadedAt(document.getUploadedAt())
                .updatedAt(document.getUpdatedAt())
                .active(document.getActive())
                .statusDisplay(document.isActive() ? "Active" : "Inactive")
                .version(document.getVersion())
                .checksum(document.getChecksum())
                .notes(document.getNotes())
                .downloadUrl("/api/documents/" + document.getId() + "/download")
                .build();
    }

    private DocumentAccessLogResponse toAccessLogResponse(DocumentAccessLog log) {
        return DocumentAccessLogResponse.builder()
                .id(log.getId())
                .documentId(log.getDocumentId())
                .accessedBy(log.getAccessedBy())
                .accessType(log.getAccessType())
                .accessIp(log.getAccessIp())
                .userAgent(log.getUserAgent())
                .accessedAt(log.getAccessedAt())
                .build();
    }
}