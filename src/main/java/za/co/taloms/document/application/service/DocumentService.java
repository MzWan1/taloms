package za.co.taloms.document.application.service;

import org.springframework.web.multipart.MultipartFile;
import za.co.taloms.document.application.dto.DocumentAccessLogResponse;
import za.co.taloms.document.application.dto.DocumentResponse;
import za.co.taloms.document.application.dto.DocumentUploadRequest;
import za.co.taloms.document.domain.entity.DocumentType;
import za.co.taloms.document.domain.entity.EntityType;
import java.util.List;

public interface DocumentService {
    DocumentResponse uploadDocument(MultipartFile file, DocumentUploadRequest request, String uploadedBy, String clientIp, String userAgent);
    DocumentResponse findById(Long id);
    DocumentResponse findByStoredFilename(String storedFilename);
    List<DocumentResponse> findAll();
    List<DocumentResponse> findByRelatedEntity(EntityType entityType, Long entityId);
    List<DocumentResponse> findByDocumentType(DocumentType documentType);
    List<DocumentResponse> findByUploadedBy(String uploadedBy);
    List<DocumentResponse> findByActive();
    DocumentResponse updateDocument(Long id, String description, String notes);
    DocumentResponse deactivateDocument(Long id, String deactivatedBy);
    DocumentResponse activateDocument(Long id, String activatedBy);
    void deleteDocument(Long id, String deletedBy);
    byte[] downloadDocument(Long id, String accessedBy, String clientIp, String userAgent);
    List<DocumentAccessLogResponse> getDocumentAccessLogs(Long documentId);
    long countByRelatedEntity(EntityType entityType, Long entityId);
    long countAll();
}