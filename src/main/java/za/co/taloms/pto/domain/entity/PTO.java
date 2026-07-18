package za.co.taloms.pto.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.context.SecurityContextHolder;
import za.co.taloms.parcel.domain.entity.Parcel;
import za.co.taloms.traditionalauthority.domain.entity.TraditionalAuthority;
import za.co.taloms.traditionalauthority.domain.entity.Village;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pto_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PTO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pto_number", nullable = false, unique = true, length = 20)
    private String ptoNumber;

    @Column(name = "pto_holder_name", nullable = false, length = 150)
    private String ptoHolderName;

    @Column(name = "id_number", nullable = false, length = 13)
    private String idNumber;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PTOPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PTOStatus status = PTOStatus.PENDING;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "village_id")
    private Village village;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traditional_authority_id")
    private TraditionalAuthority traditionalAuthority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_id")
    private Parcel parcel;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    @Column(name = "suspended_by", length = 50)
    private String suspendedBy;

    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    @Column(name = "suspend_reason", columnDefinition = "TEXT")
    private String suspendReason;

    @Column(name = "reactivated_by", length = 50)
    private String reactivatedBy;

    @Column(name = "reactivated_at")
    private LocalDateTime reactivatedAt;

    @Column(name = "reactivate_notes", columnDefinition = "TEXT")
    private String reactivateNotes;

    @Column(name = "reinstated_by", length = 50)
    private String reinstatedBy;

    @Column(name = "reinstated_at")
    private LocalDateTime reinstatedAt;

    @Column(name = "reinstate_reason", columnDefinition = "TEXT")
    private String reinstateReason;

    @Column(name = "revoked_by", length = 50)
    private String revokedBy;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoke_reason", columnDefinition = "TEXT")
    private String revokeReason;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = PTOStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void reinstate(String reason) {
        this.status = PTOStatus.ACTIVE;
        this.reinstatedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        this.reinstatedAt = LocalDateTime.now();
        this.reinstateReason = reason;
    }

    public void suspend(String reason) {
        this.status = PTOStatus.SUSPENDED;
        this.suspendedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        this.suspendedAt = LocalDateTime.now();
        this.suspendReason = reason;
    }

    public void reactivate(String notes) {
        this.status = PTOStatus.ACTIVE;
        this.reactivatedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        this.reactivatedAt = LocalDateTime.now();
        this.reactivateNotes = notes;
    }

    public boolean isPending()   { return PTOStatus.PENDING == status; }
    public boolean isActive()    { return PTOStatus.ACTIVE == status; }
    public boolean isSuspended() { return PTOStatus.SUSPENDED == status; }
    public boolean isRevoked()   { return PTOStatus.REVOKED == status; }
    public boolean isExpired()   { return PTOStatus.EXPIRED == status; }

    public boolean canBeApproved() { return isPending() || isSuspended(); }
    public boolean canBeRevoked()  { return isActive() || isSuspended(); }
    public boolean canBeSuspended(){ return isActive(); }
}