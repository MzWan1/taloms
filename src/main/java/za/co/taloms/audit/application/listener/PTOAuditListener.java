package za.co.taloms.audit.application.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import za.co.taloms.audit.application.dto.AuditLogRequest;
import za.co.taloms.audit.application.service.AuditService;
import za.co.taloms.audit.domain.entity.AuditAction;
import za.co.taloms.pto.domain.event.PTOApprovedEvent;
import za.co.taloms.pto.domain.event.PTOCreatedEvent;
import za.co.taloms.pto.domain.event.PTOReinstatedEvent;
import za.co.taloms.pto.domain.event.PTORevokedEvent;

@Slf4j
@Component
public class PTOAuditListener {

    private final AuditService auditService;

    public PTOAuditListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @Async
    @EventListener
    public void onPTOCreated(PTOCreatedEvent event) {
        try {
            String details = String.format("{\"ptoNumber\":\"%s\",\"holderName\":\"%s\",\"villageId\":%d,\"traditionalAuthorityId\":%d}",
                    event.getPtoNumber(), event.getPtoHolderName(), event.getVillageId(), event.getTraditionalAuthorityId());

            var request = AuditLogRequest.builder()
                    .entityType("PTO")
                    .entityId(event.getPtoId())
                    .action(AuditAction.CREATE)
                    .newValue(details)
                    .performedBy(event.getCreatedBy())
                    .description("PTO " + event.getPtoNumber() + " created for " + event.getPtoHolderName())
                    .build();

            auditService.logAction(request);
        } catch (Exception e) {
            log.error("Failed to log PTO created event: {}", e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void onPTOApproved(PTOApprovedEvent event) {
        try {
            String details = String.format("{\"ptoNumber\":\"%s\",\"holderName\":\"%s\",\"approvedBy\":\"%s\"}",
                    event.getPtoNumber(), event.getPtoHolderName(), event.getApprovedBy());

            var request = AuditLogRequest.builder()
                    .entityType("PTO")
                    .entityId(event.getPtoId())
                    .action(AuditAction.APPROVE)
                    .newValue(details)
                    .performedBy(event.getApprovedBy())
                    .description("PTO " + event.getPtoNumber() + " approved for " + event.getPtoHolderName())
                    .build();

            auditService.logAction(request);
        } catch (Exception e) {
            log.error("Failed to log PTO approved event: {}", e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void onPTORevoked(PTORevokedEvent event) {
        try {
            String details = String.format("{\"ptoNumber\":\"%s\",\"holderName\":\"%s\",\"revokedBy\":\"%s\",\"reason\":\"%s\"}",
                    event.getPtoNumber(), event.getPtoHolderName(), event.getRevokedBy(), event.getReason());

            var request = AuditLogRequest.builder()
                    .entityType("PTO")
                    .entityId(event.getPtoId())
                    .action(AuditAction.REVOKE)
                    .newValue(details)
                    .performedBy(event.getRevokedBy())
                    .description("PTO " + event.getPtoNumber() + " revoked - Reason: " + event.getReason())
                    .build();

            auditService.logAction(request);
        } catch (Exception e) {
            log.error("Failed to log PTO revoked event: {}", e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void onPTOReinstated(PTOReinstatedEvent event) {
        try {
            String details = String.format("{\"ptoId\":%d,\"ptoNumber\":\"%s\",\"holderName\":\"%s\",\"reinstatedBy\":\"%s\",\"reason\":\"%s\"}",
                    event.getPtoId(), event.getPtoNumber(), event.getPtoHolderName(), event.getReinstatedBy(), event.getReason());

            var request = AuditLogRequest.builder()
                    .entityType("PTO")
                    .entityId(event.getPtoId())
                    .action(AuditAction.REINSTATE)
                    .newValue(details)
                    .performedBy(event.getReinstatedBy())
                    .description("PTO " + event.getPtoNumber() + " reinstated - Reason: " + event.getReason())
                    .build();

            auditService.logAction(request);
        } catch (Exception e) {
            log.error("Failed to log PTO reinstated event: {}", e.getMessage(), e);
        }
    }
}