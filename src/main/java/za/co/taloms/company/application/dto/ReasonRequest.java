package za.co.taloms.company.application.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

/** Optional operator-supplied reason attached to an admin action. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReasonRequest {

    @Size(max = 255, message = "Reason must be at most 255 characters")
    private String reason;
}