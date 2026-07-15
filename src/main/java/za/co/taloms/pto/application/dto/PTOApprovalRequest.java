package za.co.taloms.pto.application.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PTOApprovalRequest {
    private String notes;
}