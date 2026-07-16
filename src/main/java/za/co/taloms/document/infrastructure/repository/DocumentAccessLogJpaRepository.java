package za.co.taloms.document.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.taloms.document.domain.entity.DocumentAccessLog;
import java.util.List;

public interface DocumentAccessLogJpaRepository extends JpaRepository<DocumentAccessLog, Long> {

    List<DocumentAccessLog> findByDocumentId(Long documentId);

    List<DocumentAccessLog> findByAccessedBy(String accessedBy);

    @Query("SELECT l FROM DocumentAccessLog l WHERE l.documentId = :documentId ORDER BY l.accessedAt DESC")
    List<DocumentAccessLog> findByDocumentIdOrderByAccessedAtDesc(@Param("documentId") Long documentId);

    long countByDocumentId(Long documentId);
}