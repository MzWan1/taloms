package za.co.taloms.audit.domain.repository;

import za.co.taloms.audit.domain.entity.AuditLog;
import za.co.taloms.audit.domain.entity.AuditAction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogRepositoryPort {
    AuditLog save(AuditLog auditLog);
    Optional<AuditLog> findById(Long id);
    List<AuditLog> findAll();
    Page<AuditLog> findAll(Pageable pageable);
    List<AuditLog> findByEntity(String entityType, Long entityId);
    Page<AuditLog> findByEntity(String entityType, Long entityId, Pageable pageable);
    List<AuditLog> findByEntityOrderByPerformedAtDesc(String entityType, Long entityId);
    List<AuditLog> findByPerformedBy(String performedBy);
    List<AuditLog> findByAction(AuditAction action);
    List<AuditLog> findByDateRange(LocalDateTime from, LocalDateTime to);
    List<AuditLog> findByPerformedByAndDateRange(String performedBy, LocalDateTime from, LocalDateTime to);
    List<AuditLog> findByEntityAndDateRange(String entityType, Long entityId, LocalDateTime from, LocalDateTime to);
    long countByEntity(String entityType, Long entityId);
    long countByPerformedBy(String performedBy);
    long countAll();
}

