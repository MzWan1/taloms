package za.co.taloms.traditionalauthority.application.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TraditionalAuthorityRequest {

    @NotBlank(message = "Authority name is required")
    @Size(max = 150, message = "Authority name must not exceed 150 characters")
    private String authorityName;

    @NotNull(message = "Chief is required")
    private Long chiefId;

    private Long headmanId;

    @Pattern(regexp = "^(\\+27|0)[0-9]{9}$",
            message = "Enter a valid South African phone number")
    private String contactPhone;

    @Email(message = "Enter a valid email address")
    private String contactEmail;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String physicalAddress;

    @Size(max = 100, message = "Region must not exceed 100 characters")
    private String region;

    /**
     * JSON array of mapped boundary vertices, e.g. [{"lat":-25.1,"lng":28.2},...].
     * Captured on the interactive map; stored as-is for spatial validation.
     */
    private String boundaryJson;
}

