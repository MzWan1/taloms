package za.co.taloms.document.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.document.domain.entity.DocumentAccessLog;
import za.co.taloms.document.domain.repository.DocumentAccessLogRepositoryPort;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DocumentAccessLogRepositoryAdapter implements DocumentAccessLogRepositoryPort {

    private final DocumentAccessLogJpaRepository jpaRepository;

    @Override
    public DocumentAccessLog save(DocumentAccessLog log) {
        return jpaRepository.save(log);
    }

    @Override
    public List<DocumentAccessLog> findByDocumentId(Long documentId) {
        return jpaRepository.findByDocumentId(documentId);
    }

    @Override
    public List<DocumentAccessLog> findByAccessedBy(String accessedBy) {
        return jpaRepository.findByAccessedBy(accessedBy);
    }

    @Override
    public List<DocumentAccessLog> findByDocumentIdOrderByAccessedAtDesc(Long documentId) {
        return jpaRepository.findByDocumentIdOrderByAccessedAtDesc(documentId);
    }

    @Override
    public long countByDocumentId(Long documentId) {
        return jpaRepository.countByDocumentId(documentId);
    }
}