package za.co.taloms.reporting.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 50)
    private ReportType reportType;

    @Column(name = "report_name", nullable = false, length = 200)
    private String reportName;

    @Column(name = "schedule_type", nullable = false, length = 20)
    private String scheduleType;

    @Column(name = "schedule_config", columnDefinition = "JSONB")
    private String scheduleConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 10)
    @Builder.Default
    private ReportFormat format = ReportFormat.PDF;

    @Column(name = "recipient_email", nullable = false, length = 200)
    private String recipientEmail;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) active = true;
        if (format == null) format = ReportFormat.PDF;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}