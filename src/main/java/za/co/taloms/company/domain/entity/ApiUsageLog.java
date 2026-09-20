package za.co.taloms.company.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A single external API request record.
 *
 * This table is deliberately SEPARATE from {@code audit_logs}: the audit trail
 * covers TALOMS users acting on TALOMS entities, while this table records API
 * consumption by external companies.
 *
 * Privacy: the full ID number is never stored. {@link #idNumberHash} holds a
 * SHA-256 digest used only to correlate repeat lookups.
 */
@Entity
@Table(name = "api_usage_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null when the request failed before a company could be identified. */
    @Column(name = "company_id")
    private Long companyId;

    /** Null when the request failed before a key could be identified. */
    @Column(name = "api_key_id")
    private Long apiKeyId;

    @Column(name = "endpoint", nullable = false, length = 255)
    private String endpoint;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "required_scope", length = 30)
    private String requiredScope;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private ApiOutcome outcome;

    @Column(name = "response_status", nullable = false)
    private Integer responseStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 40)
    private ApiFailureReason failureReason;

    @Column(name = "search_type", length = 50)
    private String searchType;

    @Column(name = "masked_search_value", length = 255)
    private String maskedSearchValue;

    /** SHA-256 of the queried ID number — never the ID number itself. */
    @Column(name = "id_number_hash", length = 64)
    private String idNumberHash;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null)
            requestedAt = LocalDateTime.now();
    }

    public boolean isSuccess() {
        return ApiOutcome.SUCCESS == outcome;
    }
}