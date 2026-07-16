package za.co.taloms.document.domain.repository;

import za.co.taloms.document.domain.entity.Document;
import za.co.taloms.document.domain.entity.DocumentType;
import za.co.taloms.document.domain.entity.EntityType;
import java.util.List;
import java.util.Optional;

public interface DocumentRepositoryPort {
    Document save(Document document);
    Optional<Document> findById(Long id);
    Optional<Document> findByStoredFilename(String storedFilename);
    List<Document> findAll();
    List<Document> findByRelatedEntity(EntityType entityType, Long entityId);
    List<Document> findByDocumentType(DocumentType documentType);
    List<Document> findByUploadedBy(String uploadedBy);
    List<Document> findByActiveTrue();
    List<Document> findByRelatedEntityAndDocumentType(EntityType entityType, Long entityId, DocumentType documentType);
    List<Document> findByRelatedEntityOrderByUploadedAtDesc(EntityType entityType, Long entityId);
    long countByRelatedEntity(EntityType entityType, Long entityId);
    long countAll();
    void delete(Document document);
}