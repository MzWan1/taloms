package za.co.taloms.dashboard.application.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDto {
    private String action;
    private String entityType;
    private String entityTypeDisplay;
    private Long entityId;
    private String performedBy;
    private LocalDateTime performedAt;
    private String performedAtDisplay;
    private String description;
}