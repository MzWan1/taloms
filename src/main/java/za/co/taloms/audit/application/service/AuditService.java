package za.co.taloms.audit.application.service;

import za.co.taloms.audit.application.dto.AuditLogRequest;
import za.co.taloms.audit.application.dto.AuditLogResponse;
import za.co.taloms.audit.application.dto.AuditSearchCriteria;
import za.co.taloms.audit.domain.entity.AuditAction;
import java.util.List;

public interface AuditService {
    void logAction(AuditLogRequest request);
    AuditLogResponse findById(Long id);
    List<AuditLogResponse> findAll();
    List<AuditLogResponse> findByEntity(String entityType, Long entityId);
    List<AuditLogResponse> findByPerformedBy(String performedBy);
    List<AuditLogResponse> findByAction(AuditAction action);
    List<AuditLogResponse> search(AuditSearchCriteria criteria);
    long countByEntity(String entityType, Long entityId);
    long countByPerformedBy(String performedBy);
    long countAll();
}