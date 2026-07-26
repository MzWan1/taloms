package za.co.taloms.notification.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.notification.domain.entity.Notification;
import za.co.taloms.notification.domain.entity.NotificationChannel;
import za.co.taloms.notification.domain.entity.NotificationType;
import za.co.taloms.notification.domain.repository.NotificationRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepositoryPort notificationRepository;

    @Override
    public Notification sendEmail(String to, String subject, String body, String entityType, Long entityId) {
        return createAndSend(NotificationType.DOCUMENT_UPLOADED, NotificationChannel.EMAIL,
                to, subject, body, entityType, entityId);
    }

    @Override
    public Notification sendSms(String to, String body, String entityType, Long entityId) {
        return createAndSend(NotificationType.DOCUMENT_UPLOADED, NotificationChannel.SMS,
                to, null, body, entityType, entityId);
    }

    @Override
    public Notification sendInApp(String recipient, String subject, String body, String entityType, Long entityId) {
        return createAndSend(NotificationType.DOCUMENT_UPLOADED, NotificationChannel.IN_APP,
                recipient, subject, body, entityType, entityId);
    }

    @Override
    public List<Notification> findByEntity(String entityType, Long entityId) {
        return notificationRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    @Override
    public List<Notification> findByRecipient(String recipient) {
        return notificationRepository.findByRecipient(recipient);
    }

    @Override
    public List<Notification> findPending() {
        return notificationRepository.findPendingNotifications();
    }

    @Override
    public List<Notification> findAll() {
        return notificationRepository.findAll();
    }

    @Override
    @Transactional
    public void processPendingNotifications() {
        List<Notification> pending = notificationRepository.findPendingNotifications();
        for (Notification notification : pending) {
            try {
                // Simulate sending — in production integrate with email/SMS provider
                log.info("Processing {} notification to {}",
                        notification.getChannel(), notification.getRecipient());
                notification.setSentAt(LocalDateTime.now());
                notification.setDeliveredAt(LocalDateTime.now());
                notificationRepository.save(notification);
            } catch (Exception e) {
                log.error("Failed to send notification {}: {}", notification.getId(), e.getMessage());
                notification.setFailedAt(LocalDateTime.now());
                notification.setFailureReason(e.getMessage());
                notificationRepository.save(notification);
            }
        }
    }

    private Notification createAndSend(NotificationType type, NotificationChannel channel,
                                       String recipient, String subject, String body,
                                       String entityType, Long entityId) {
        var notification = Notification.builder()
                .notificationType(type)
                .channel(channel)
                .recipient(recipient)
                .subject(subject)
                .body(body)
                .entityType(entityType)
                .entityId(entityId)
                .build();

        return notificationRepository.save(notification);
    }
}


