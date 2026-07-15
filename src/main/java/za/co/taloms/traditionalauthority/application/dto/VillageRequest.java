package za.co.taloms.traditionalauthority.application.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VillageRequest {

    @NotBlank(message = "Village name is required")
    @Size(max = 150, message = "Village name must not exceed 150 characters")
    private String villageName;

    @Size(max = 100, message = "Region must not exceed 100 characters")
    private String region;

    @Size(max = 150, message = "Headman name must not exceed 150 characters")
    private String headmanName;

    private String description;

    @NotNull(message = "Traditional Authority is required")
    private Long traditionalAuthorityId;
}