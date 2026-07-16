package za.co.taloms.gis.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gis_layers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GisLayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "layer_type", nullable = false, length = 30)
    private LayerType layerType;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "visible")
    @Builder.Default
    private Boolean visible = true;

    @Column(name = "opacity", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal opacity = BigDecimal.ONE;

    @Column(name = "z_index")
    @Builder.Default
    private Integer zIndex = 0;

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
        if (visible == null) visible = true;
        if (opacity == null) opacity = BigDecimal.ONE;
        if (zIndex == null) zIndex = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}