package za.co.taloms.notification.application.service;

import za.co.taloms.notification.domain.entity.Notification;
import za.co.taloms.notification.domain.entity.NotificationChannel;
import za.co.taloms.notification.domain.entity.NotificationType;
import java.util.List;

public interface NotificationService {
    Notification sendEmail(String to, String subject, String body, String entityType, Long entityId);
    Notification sendSms(String to, String body, String entityType, Long entityId);
    Notification sendInApp(String recipient, String subject, String body, String entityType, Long entityId);
    List<Notification> findByEntity(String entityType, Long entityId);
    List<Notification> findByRecipient(String recipient);
    List<Notification> findPending();
    void processPendingNotifications();
    List<Notification> findAll();
}


