package za.co.taloms.parcel.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.traditionalauthority.domain.entity.Village;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parcels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parcel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parcel_number", nullable = false, unique = true, length = 20)
    private String parcelNumber;

    @Column(name = "stand_number", nullable = false, length = 20)
    private String standNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "parcel_type", nullable = false, length = 30)
    private ParcelType parcelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ParcelStatus status = ParcelStatus.AVAILABLE;

    @Column(name = "area_m2")
    private Double areaM2;

    @Column(name = "area_hectares")
    private Double areaHectares;

    @Column(name = "centroid_lat")
    private Double centroidLat;

    @Column(name = "centroid_lng")
    private Double centroidLng;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "village_id", nullable = false)
    private Village village;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pto_id")
    private PTO pto;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "parcel", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    @Builder.Default
    private List<ParcelBoundary> boundaries = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ParcelStatus.AVAILABLE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isAvailable() { return ParcelStatus.AVAILABLE == status; }
    public boolean isAllocated() { return ParcelStatus.ALLOCATED == status; }
    public boolean isDisputed() { return ParcelStatus.DISPUTED == status; }
    public boolean isReserved() { return ParcelStatus.RESERVED == status; }
    public boolean isInactive() { return ParcelStatus.INACTIVE == status; }

    public boolean canBeAllocated() { return isAvailable() || isReserved(); }
    public boolean canBeDisputed() { return isAllocated(); }
    public boolean canBeReserved() { return isAvailable(); }
}