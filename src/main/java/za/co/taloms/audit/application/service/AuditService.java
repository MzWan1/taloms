package za.co.taloms.audit.application.service;

import za.co.taloms.audit.application.dto.AuditLogRequest;
import za.co.taloms.audit.application.dto.AuditLogResponse;
import za.co.taloms.audit.application.dto.AuditSearchCriteria;
import za.co.taloms.audit.domain.entity.AuditAction;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {
    void logAction(AuditLogRequest request);
    AuditLogResponse findById(Long id);
    List<AuditLogResponse> findAll();
    Page<AuditLogResponse> findAll(Pageable pageable);
    List<AuditLogResponse> findByEntity(String entityType, Long entityId);
    Page<AuditLogResponse> findByEntity(String entityType, Long entityId, Pageable pageable);
    List<AuditLogResponse> findByPerformedBy(String performedBy);
    List<AuditLogResponse> findByAction(AuditAction action);
    List<AuditLogResponse> search(AuditSearchCriteria criteria);
    List<AuditLogResponse> findRecent(int limit);
    long countByEntity(String entityType, Long entityId);
    long countByPerformedBy(String performedBy);
    long countAll();
}

