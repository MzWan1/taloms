package za.co.taloms.pto.domain.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import za.co.taloms.notification.application.service.NotificationService;
import za.co.taloms.notification.domain.entity.NotificationChannel;
import za.co.taloms.notification.domain.entity.NotificationType;
import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.event.PTOApprovedEvent;
import za.co.taloms.pto.domain.event.PTOCreatedEvent;
import za.co.taloms.pto.domain.event.PTOExpiredEvent;
import za.co.taloms.pto.domain.event.PTOReinstatedEvent;
import za.co.taloms.pto.domain.event.PTORevokedEvent;
import za.co.taloms.pto.domain.event.PTOSuspendedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class PTONotificationListener {

    private final NotificationService notificationService;

    @EventListener
    public void onPTOCreated(PTOCreatedEvent event) {
        log.info("Sending PTO_CREATED notification for PTO {}", event.getPtoNumber());
        notificationService.sendInApp(
                "system",
                "New PTO Created: " + event.getPtoNumber(),
                "A new PTO has been created for " + event.getPtoHolderName() +
                        " in village " + event.getVillageId(),
                "PTO",
                event.getPtoId()
        );
    }

    @EventListener
    public void onPTOApproved(PTOApprovedEvent event) {
        log.info("Sending PTO_APPROVED notification for PTO {}", event.getPtoNumber());
        notificationService.sendInApp(
                "system",
                "PTO Approved: " + event.getPtoNumber(),
                "PTO for " + event.getPtoHolderName() + " was approved by " + event.getApprovedBy(),
                "PTO",
                event.getPtoId()
        );
    }

    @EventListener
    public void onPTORevoked(PTORevokedEvent event) {
        log.info("Sending PTO_REVOKED notification for PTO {}", event.getPtoNumber());
        notificationService.sendInApp(
                "system",
                "PTO Revoked: " + event.getPtoNumber(),
                "PTO for " + event.getPtoHolderName() + " was revoked. Reason: " + event.getReason(),
                "PTO",
                event.getPtoId()
        );
    }

    @EventListener
    public void onPTOSuspended(PTOSuspendedEvent event) {
        log.info("Sending PTO_SUSPENDED notification for PTO {}", event.getPtoNumber());
        notificationService.sendInApp(
                "system",
                "PTO Suspended: " + event.getPtoNumber(),
                "PTO for " + event.getPtoHolderName() + " was suspended. Reason: " + event.getReason(),
                "PTO",
                event.getPtoId()
        );
    }

    @EventListener
    public void onPTOReinstated(PTOReinstatedEvent event) {
        log.info("Sending PTO_REINSTATED notification for PTO {}", event.getPtoNumber());
        notificationService.sendInApp(
                "system",
                "PTO Reinstated: " + event.getPtoNumber(),
                "PTO for " + event.getPtoHolderName() + " was reinstated",
                "PTO",
                event.getPtoId()
        );
    }

    @EventListener
    public void onPTOExpired(PTOExpiredEvent event) {
        log.info("Sending PTO_EXPIRED notification for PTO {}", event.getPtoNumber());
        notificationService.sendInApp(
                "system",
                "PTO Expired: " + event.getPtoNumber(),
                "PTO for " + event.getPtoHolderName() + " expired on " + event.getExpiredAt(),
                "PTO",
                event.getPtoId()
        );
    }
}


