package za.co.taloms.useraccess.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * A delegated "linked" access link between a user account and a specific PTO.
 *
 * Ownership of a PTO is NOT stored here — it is derived from the SA ID-number
 * match between {@code users.id_number} and {@code pto_records.id_number}.
 * Rows in this table therefore represent additional users an owner has chosen
 * to give access to a proof of residence for one specific PTO.
 */
@Entity
@Table(name = "user_pto_access",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_pto_access", columnNames = {"user_id", "pto_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPTOAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "pto_id", nullable = false)
    private Long ptoId;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}