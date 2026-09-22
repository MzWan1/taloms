package za.co.taloms.audit.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.audit.application.dto.AuditLogRequest;
import za.co.taloms.audit.application.dto.AuditLogResponse;
import za.co.taloms.audit.application.dto.AuditSearchCriteria;
import za.co.taloms.audit.domain.entity.AuditLog;
import za.co.taloms.audit.domain.entity.AuditAction;
import za.co.taloms.audit.domain.repository.AuditLogRepositoryPort;
import za.co.taloms.common.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepositoryPort auditRepository;

    @Override
    @Async
    public void logAction(AuditLogRequest request) {
        try {
            var auditLog = AuditLog.builder()
                    .entityType(request.getEntityType())
                    .entityId(request.getEntityId())
                    .action(request.getAction())
                    .previousValue(request.getPreviousValue())
                    .newValue(request.getNewValue())
                    .performedBy(request.getPerformedBy())
                    .ipAddress(request.getIpAddress())
                    .userAgent(request.getUserAgent())
                    .description(request.getDescription())
                    .performedAt(LocalDateTime.now())
                    .build();

            auditRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log audit action", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse findById(Long id) {
        AuditLog log = auditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found with id: " + id));
        return toResponse(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> findAll() {
        return auditRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findAll(Pageable pageable) {
        return auditRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> findByEntity(String entityType, Long entityId) {
        return auditRepository.findByEntity(entityType, entityId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findByEntity(String entityType, Long entityId, Pageable pageable) {
        return auditRepository.findByEntity(entityType, entityId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> findByPerformedBy(String performedBy) {
        return auditRepository.findByPerformedBy(performedBy).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> findByAction(AuditAction action) {
        return auditRepository.findByAction(action).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> search(AuditSearchCriteria criteria) {
        List<AuditLog> logs = auditRepository.findAll();

        if (criteria.getEntityType() != null && !criteria.getEntityType().isEmpty()) {
            logs = logs.stream()
                    .filter(a -> a.getEntityType() != null && a.getEntityType().equalsIgnoreCase(criteria.getEntityType()))
                    .collect(Collectors.toList());
        }

        if (criteria.getEntityId() != null) {
            logs = logs.stream()
                    .filter(a -> a.getEntityId().equals(criteria.getEntityId()))
                    .collect(Collectors.toList());
        }

        if (criteria.getAction() != null) {
            logs = logs.stream()
                    .filter(a -> a.getAction() == criteria.getAction())
                    .collect(Collectors.toList());
        }

        if (criteria.getPerformedBy() != null && !criteria.getPerformedBy().isEmpty()) {
            logs = logs.stream()
                    .filter(a -> a.getPerformedBy().toLowerCase().contains(criteria.getPerformedBy().toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (criteria.getDateFrom() != null) {
            logs = logs.stream()
                    .filter(a -> a.getPerformedAt().isAfter(criteria.getDateFrom().minusSeconds(1)))
                    .collect(Collectors.toList());
        }

        if (criteria.getDateTo() != null) {
            logs = logs.stream()
                    .filter(a -> a.getPerformedAt().isBefore(criteria.getDateTo().plusSeconds(1)))
                    .collect(Collectors.toList());
        }

        return logs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> findRecent(int limit) {
        List<AuditLog> logs = auditRepository.findByEntityOrderByPerformedAtDesc(null, null);
        if (logs.size() > limit) {
            logs = logs.subList(0, limit);
        }
        return logs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByEntity(String entityType, Long entityId) {
        return auditRepository.countByEntity(entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByPerformedBy(String performedBy) {
        return auditRepository.countByPerformedBy(performedBy);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return auditRepository.countAll();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .entityType(log.getEntityType())
                .entityTypeDisplay(getEntityTypeDisplay(log.getEntityType()))
                .entityId(log.getEntityId())
                .action(log.getAction())
                .actionDisplay(log.getAction().getDisplayName())
                .actionBadgeClass(log.getAction().getBadgeClass())
                .previousValue(log.getPreviousValue())
                .newValue(log.getNewValue())
                .performedBy(log.getPerformedBy())
                .performedAt(log.getPerformedAt())
                .performedAtDisplay(log.getPerformedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")))
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .description(log.getDescription())
                .build();
    }

    private String getEntityTypeDisplay(String entityType) {
        if (entityType == null) return "";
        return switch (entityType.toUpperCase()) {
            case "PTO" -> "PTO";
            case "PARCEL" -> "Parcel";
            case "RESIDENT" -> "Resident";
            case "HOUSEHOLD" -> "Household";
            case "BUSINESS" -> "Business";
            case "USER" -> "User";
            case "AUTHORITY" -> "Traditional Authority";
            case "VILLAGE" -> "Village";
            case "DOCUMENT" -> "Document";
            default -> entityType;
        };
    }
}
