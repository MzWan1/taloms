package za.co.taloms.traditionalauthority.application.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TraditionalAuthorityRequest {

    @NotBlank(message = "Authority name is required")
    @Size(max = 150, message = "Authority name must not exceed 150 characters")
    private String authorityName;

    @NotBlank(message = "Chief name is required")
    @Size(max = 150, message = "Chief name must not exceed 150 characters")
    private String chiefName;

    @Size(max = 150, message = "Headman name must not exceed 150 characters")
    private String headmanName;

    @Pattern(regexp = "^(\\+27|0)[0-9]{9}$",
            message = "Enter a valid South African phone number")
    private String contactPhone;

    @Email(message = "Enter a valid email address")
    private String contactEmail;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String physicalAddress;

    @Size(max = 100, message = "Region must not exceed 100 characters")
    private String region;
}