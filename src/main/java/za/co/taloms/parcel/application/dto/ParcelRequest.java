package za.co.taloms.parcel.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import za.co.taloms.parcel.domain.entity.CaptureMode;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelRequest {

    @NotBlank(message = "Stand number is required")
    @Size(max = 20, message = "Stand number must not exceed 20 characters")
    private String standNumber;


    @NotNull(message = "Village is required")
    private Long villageId;

    private Long ptoId;

    private String notes;

    @NotNull(message = "Boundary points are required")
    @Size(min = 3, message = "A polygon must have at least 3 points")
    private List<BoundaryPointDto> boundaries;

    private CaptureMode captureMode = CaptureMode.MANUAL_TAP;
}

