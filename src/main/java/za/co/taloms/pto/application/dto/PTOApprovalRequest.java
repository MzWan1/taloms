package za.co.taloms.pto.application.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PTOApprovalRequest {
    private String notes;
    private String signatureData;
    private String signatureImagePath;
    private String ipAddress;
    private String userAgent;
}

