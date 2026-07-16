package za.co.taloms.document.domain.repository;

import za.co.taloms.document.domain.entity.DocumentAccessLog;
import java.util.List;

public interface DocumentAccessLogRepositoryPort {
    DocumentAccessLog save(DocumentAccessLog log);
    List<DocumentAccessLog> findByDocumentId(Long documentId);
    List<DocumentAccessLog> findByAccessedBy(String accessedBy);
    List<DocumentAccessLog> findByDocumentIdOrderByAccessedAtDesc(Long documentId);
    long countByDocumentId(Long documentId);
}