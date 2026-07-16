package za.co.taloms.audit.application.dto;

import lombok.*;
import za.co.taloms.audit.domain.entity.AuditAction;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditSearchCriteria {

    private String entityType;
    private Long entityId;
    private AuditAction action;
    private String performedBy;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private Integer page;
    private Integer size;
}