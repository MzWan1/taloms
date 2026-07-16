package za.co.taloms.document.application.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAccessLogResponse {

    private Long id;
    private Long documentId;
    private String accessedBy;
    private String accessType;
    private String accessIp;
    private String userAgent;
    private LocalDateTime accessedAt;
}