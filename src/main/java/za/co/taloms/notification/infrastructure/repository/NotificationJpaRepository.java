package za.co.taloms.notification.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.taloms.notification.domain.entity.Notification;
import za.co.taloms.notification.domain.entity.NotificationChannel;
import java.util.List;

public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    List<Notification> findByRecipientOrderByCreatedAtDesc(String recipient);

    List<Notification> findByChannelOrderByCreatedAtDesc(NotificationChannel channel);

    @Query("SELECT n FROM Notification n WHERE n.sentAt IS NULL ORDER BY n.createdAt ASC")
    List<Notification> findPendingNotifications();
}


