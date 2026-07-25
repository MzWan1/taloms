package za.co.taloms.notification.domain.repository;

import za.co.taloms.notification.domain.entity.Notification;
import za.co.taloms.notification.domain.entity.NotificationChannel;
import java.util.List;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);
    List<Notification> findAll();
    List<Notification> findByEntityTypeAndEntityId(String entityType, Long entityId);
    List<Notification> findByRecipient(String recipient);
    List<Notification> findByChannel(NotificationChannel channel);
    List<Notification> findPendingNotifications();
}
