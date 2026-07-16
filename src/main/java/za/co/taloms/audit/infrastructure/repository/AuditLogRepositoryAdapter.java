package za.co.taloms.audit.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.audit.domain.entity.AuditLog;
import za.co.taloms.audit.domain.entity.AuditAction;
import za.co.taloms.audit.domain.repository.AuditLogRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepositoryPort {

    private final AuditLogJpaRepository jpaRepository;

    @Override
    public AuditLog save(AuditLog auditLog) {
        return jpaRepository.save(auditLog);
    }

    @Override
    public Optional<AuditLog> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<AuditLog> findAll() {
        return jpaRepository.findAllOrderByPerformedAtDesc();
    }

    @Override
    public List<AuditLog> findByEntity(String entityType, Long entityId) {
        return jpaRepository.findByEntityOrderByPerformedAtDesc(entityType, entityId);
    }

    @Override
    public List<AuditLog> findByEntityOrderByPerformedAtDesc(String entityType, Long entityId) {
        return jpaRepository.findByEntityOrderByPerformedAtDesc(entityType, entityId);
    }

    @Override
    public List<AuditLog> findByPerformedBy(String performedBy) {
        return jpaRepository.findByPerformedBy(performedBy);
    }

    @Override
    public List<AuditLog> findByAction(AuditAction action) {
        return jpaRepository.findByAction(action);
    }

    @Override
    public List<AuditLog> findByDateRange(LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByPerformedAtBetween(from, to);
    }

    @Override
    public List<AuditLog> findByPerformedByAndDateRange(String performedBy, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByPerformedByAndPerformedAtBetween(performedBy, from, to);
    }

    @Override
    public List<AuditLog> findByEntityAndDateRange(String entityType, Long entityId, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByEntityAndPerformedAtBetween(entityType, entityId, from, to);
    }

    @Override
    public long countByEntity(String entityType, Long entityId) {
        return jpaRepository.countByEntityTypeAndEntityId(entityType, entityId);
    }

    @Override
    public long countByPerformedBy(String performedBy) {
        return jpaRepository.countByPerformedBy(performedBy);
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }
}