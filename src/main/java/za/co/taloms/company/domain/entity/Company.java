package za.co.taloms.company.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * An external organisation that has been explicitly authorised by an ADMIN to
 * use the TALOMS partner API.
 *
 * The company is intentionally NOT a {@code User} record: it is an organisation
 * that authenticates with API keys, never with a username/password. Its
 * {@link #status} is the master switch — disabling it instantly invalidates
 * every API key it owns.
 */
@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "registration_number", unique = true, length = 50)
    private String registrationNumber;

    @Column(name = "contact_email", nullable = false, length = 150)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CompanyStatus status = CompanyStatus.ACTIVE;

    @Column(name = "disabled_reason", columnDefinition = "TEXT")
    private String disabledReason;

    @Column(name = "disabled_at")
    private LocalDateTime disabledAt;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = CompanyStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return CompanyStatus.ACTIVE == status;
    }

    /** Admin action: immediately revoke all API access for this company. */
    public void disable(String reason) {
        this.status = CompanyStatus.DISABLED;
        this.disabledReason = reason;
        this.disabledAt = LocalDateTime.now();
    }

    /** Admin action: restore API access. Never callable by the company itself. */
    public void activate() {
        this.status = CompanyStatus.ACTIVE;
        this.disabledReason = null;
        this.disabledAt = null;
    }
}