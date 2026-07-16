package za.co.taloms.document.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.document.domain.entity.Document;
import za.co.taloms.document.domain.entity.DocumentType;
import za.co.taloms.document.domain.entity.EntityType;
import za.co.taloms.document.domain.repository.DocumentRepositoryPort;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DocumentRepositoryAdapter implements DocumentRepositoryPort {

    private final DocumentJpaRepository jpaRepository;

    @Override
    public Document save(Document document) {
        return jpaRepository.save(document);
    }

    @Override
    public Optional<Document> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Document> findByStoredFilename(String storedFilename) {
        return jpaRepository.findByStoredFilename(storedFilename);
    }

    @Override
    public List<Document> findAll() {
        return jpaRepository.findAllOrderByUploadedAtDesc();
    }

    @Override
    public List<Document> findByRelatedEntity(EntityType entityType, Long entityId) {
        return jpaRepository.findByRelatedEntityTypeAndRelatedEntityId(entityType, entityId);
    }

    @Override
    public List<Document> findByDocumentType(DocumentType documentType) {
        return jpaRepository.findByDocumentType(documentType);
    }

    @Override
    public List<Document> findByUploadedBy(String uploadedBy) {
        return jpaRepository.findByUploadedBy(uploadedBy);
    }

    @Override
    public List<Document> findByActiveTrue() {
        return jpaRepository.findByActiveTrue();
    }

    @Override
    public List<Document> findByRelatedEntityAndDocumentType(EntityType entityType, Long entityId, DocumentType documentType) {
        return jpaRepository.findByRelatedEntityAndDocumentType(entityType, entityId, documentType);
    }

    @Override
    public List<Document> findByRelatedEntityOrderByUploadedAtDesc(EntityType entityType, Long entityId) {
        return jpaRepository.findByRelatedEntityOrderByUploadedAtDesc(entityType, entityId);
    }

    @Override
    public long countByRelatedEntity(EntityType entityType, Long entityId) {
        return jpaRepository.countByRelatedEntityTypeAndRelatedEntityId(entityType, entityId);
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }

    @Override
    public void delete(Document document) {
        jpaRepository.delete(document);
    }
}