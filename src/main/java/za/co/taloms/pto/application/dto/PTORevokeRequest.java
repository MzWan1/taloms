package za.co.taloms.pto.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PTORevokeRequest {

    @NotBlank(message = "Revoke reason is required")
    private String reason;
}