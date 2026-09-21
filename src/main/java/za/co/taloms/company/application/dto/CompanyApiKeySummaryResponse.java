package za.co.taloms.company.application.dto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.*;
import za.co.taloms.common.MaskedLongSerializer;
import za.co.taloms.company.domain.entity.ApiKeyStatus;
import za.co.taloms.company.domain.entity.ApiScope;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyApiKeySummaryResponse {

    @JsonSerialize(using = MaskedLongSerializer.class)
    private Long id;
    @JsonSerialize(using = MaskedLongSerializer.class)
    private Long companyId;
    private String label;
    private String keyPrefix;
    private Set<ApiScope> scopes;
    private ApiKeyStatus status;
    private String statusDisplay;
    private String statusBadgeClass;
    private LocalDateTime lastUsedAt;
    private String revokedBy;
    private String revokeReason;
    private LocalDateTime revokedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private Long totalRequests;
}
