package za.co.taloms.reporting.application.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private byte[] content;
    private String filename;
    private String contentType;
    private long fileSize;
    private LocalDateTime generatedAt;
}