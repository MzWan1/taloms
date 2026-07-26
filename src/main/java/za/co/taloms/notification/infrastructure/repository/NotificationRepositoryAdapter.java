package za.co.taloms.notification.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.notification.domain.entity.Notification;
import za.co.taloms.notification.domain.entity.NotificationChannel;
import za.co.taloms.notification.domain.repository.NotificationRepositoryPort;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository jpaRepository;

    @Override
    public Notification save(Notification notification) {
        return jpaRepository.save(notification);
    }

    @Override
    public List<Notification> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Notification> findByEntityTypeAndEntityId(String entityType, Long entityId) {
        return jpaRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    @Override
    public List<Notification> findByRecipient(String recipient) {
        return jpaRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }

    @Override
    public List<Notification> findByChannel(NotificationChannel channel) {
        return jpaRepository.findByChannelOrderByCreatedAtDesc(channel);
    }

    @Override
    public List<Notification> findPendingNotifications() {
        return jpaRepository.findPendingNotifications();
    }
}


