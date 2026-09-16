package za.co.taloms.dashboard.application.dto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.*;
import za.co.taloms.common.MaskedLongSerializer;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDto {
    private String action;
    private String actionDisplay;
    private String badgeClass;
    private String entityType;
    private String entityTypeDisplay;
    @JsonSerialize(using = MaskedLongSerializer.class)
    private Long entityId;
    private String performedBy;
    private LocalDateTime performedAt;
    private String performedAtDisplay;
    private String description;
}

