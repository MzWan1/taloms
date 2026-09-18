package za.co.taloms.company.application.dto;

import lombok.*;
import za.co.taloms.company.domain.entity.ApiFailureReason;
import za.co.taloms.company.domain.entity.ApiOutcome;

/**
 * Internal input for writing one API usage record. Assembled by the API
 * authentication filter (which alone sees the whole request lifecycle) and
 * written by {@code ApiUsageService}.
 *
 * There is deliberately no field for the raw ID number: callers supply only the
 * SHA-256 {@link #idNumberHash}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiUsageRecord {

    private Long companyId;
    private Long apiKeyId;
    private String endpoint;
    private String httpMethod;
    private String requiredScope;
    private ApiOutcome outcome;
    private Integer responseStatus;
    private ApiFailureReason failureReason;
    private String idNumberHash;
    private String clientIp;
    private String userAgent;
    private Integer durationMs;
}