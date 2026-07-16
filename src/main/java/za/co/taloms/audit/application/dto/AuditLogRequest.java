package za.co.taloms.audit.application.dto;

import lombok.*;
import za.co.taloms.audit.domain.entity.AuditAction;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogRequest {

    private String entityType;
    private Long entityId;
    private AuditAction action;
    private String previousValue;
    private String newValue;
    private String performedBy;
    private String ipAddress;
    private String userAgent;
    private String description;
}