package za.co.taloms.audit.application.dto;

import lombok.*;
import za.co.taloms.audit.domain.entity.AuditAction;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private String entityType;
    private String entityTypeDisplay;
    private Long entityId;
    private AuditAction action;
    private String actionDisplay;
    private String actionBadgeClass;
    private String previousValue;
    private String newValue;
    private String performedBy;
    private LocalDateTime performedAt;
    private String performedAtDisplay;
    private String ipAddress;
    private String userAgent;
    private String description;
}