package za.co.taloms.document.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.taloms.document.domain.entity.Document;
import za.co.taloms.document.domain.entity.DocumentType;
import za.co.taloms.document.domain.entity.EntityType;
import java.util.List;
import java.util.Optional;

public interface DocumentJpaRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByStoredFilename(String storedFilename);

    List<Document> findByRelatedEntityTypeAndRelatedEntityId(EntityType entityType, Long entityId);

    List<Document> findByDocumentType(DocumentType documentType);

    List<Document> findByUploadedBy(String uploadedBy);

    List<Document> findByActiveTrue();

    @Query("SELECT d FROM Document d WHERE d.relatedEntityType = :entityType AND d.relatedEntityId = :entityId AND d.documentType = :documentType")
    List<Document> findByRelatedEntityAndDocumentType(@Param("entityType") EntityType entityType,
                                                      @Param("entityId") Long entityId,
                                                      @Param("documentType") DocumentType documentType);

    @Query("SELECT d FROM Document d WHERE d.relatedEntityType = :entityType AND d.relatedEntityId = :entityId ORDER BY d.uploadedAt DESC")
    List<Document> findByRelatedEntityOrderByUploadedAtDesc(@Param("entityType") EntityType entityType,
                                                            @Param("entityId") Long entityId);

    long countByRelatedEntityTypeAndRelatedEntityId(EntityType entityType, Long entityId);

    @Query("SELECT d FROM Document d ORDER BY d.uploadedAt DESC")
    List<Document> findAllOrderByUploadedAtDesc();
}