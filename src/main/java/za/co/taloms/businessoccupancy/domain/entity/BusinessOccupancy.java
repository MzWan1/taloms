package za.co.taloms.businessoccupancy.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import za.co.taloms.parcel.domain.entity.Parcel;
import za.co.taloms.pto.domain.entity.PTO;
import java.time.LocalDateTime;

@Entity
@Table(name = "business_occupancies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessOccupancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 50)
    private BusinessType businessType;

    @Column(name = "owner_name", nullable = false, length = 150)
    private String ownerName;

    @Column(name = "owner_id_number", nullable = false, length = 13)
    private String ownerIdNumber;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_id", nullable = false)
    private Parcel parcel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pto_id", nullable = false)
    private PTO pto;

    @Column(name = "operating_hours", length = 200)
    private String operatingHours;

    @Column(name = "employees_count")
    @Builder.Default
    private Integer employeesCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private BusinessStatus status = BusinessStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String notes;

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
        if (status == null) status = BusinessStatus.ACTIVE;
        if (employeesCount == null) employeesCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isActive() { return BusinessStatus.ACTIVE == status; }
    public boolean isInactive() { return BusinessStatus.INACTIVE == status; }
    public boolean isPending() { return BusinessStatus.PENDING == status; }
    public boolean isSuspended() { return BusinessStatus.SUSPENDED == status; }
}