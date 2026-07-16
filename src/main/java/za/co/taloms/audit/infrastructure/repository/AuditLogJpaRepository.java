package za.co.taloms.audit.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.taloms.audit.domain.entity.AuditLog;
import za.co.taloms.audit.domain.entity.AuditAction;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogJpaRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.performedAt DESC")
    List<AuditLog> findByEntityOrderByPerformedAtDesc(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    List<AuditLog> findByPerformedBy(String performedBy);

    List<AuditLog> findByAction(AuditAction action);

    @Query("SELECT a FROM AuditLog a WHERE a.performedAt BETWEEN :from AND :to ORDER BY a.performedAt DESC")
    List<AuditLog> findByPerformedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT a FROM AuditLog a WHERE a.performedBy = :performedBy AND a.performedAt BETWEEN :from AND :to ORDER BY a.performedAt DESC")
    List<AuditLog> findByPerformedByAndPerformedAtBetween(@Param("performedBy") String performedBy,
                                                          @Param("from") LocalDateTime from,
                                                          @Param("to") LocalDateTime to);

    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId AND a.performedAt BETWEEN :from AND :to ORDER BY a.performedAt DESC")
    List<AuditLog> findByEntityAndPerformedAtBetween(@Param("entityType") String entityType,
                                                     @Param("entityId") Long entityId,
                                                     @Param("from") LocalDateTime from,
                                                     @Param("to") LocalDateTime to);

    long countByEntityTypeAndEntityId(String entityType, Long entityId);

    long countByPerformedBy(String performedBy);

    @Query("SELECT a FROM AuditLog a ORDER BY a.performedAt DESC")
    List<AuditLog> findAllOrderByPerformedAtDesc();
}