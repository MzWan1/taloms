package za.co.taloms.company.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Admin request to register a new external company. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyCreateRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 150, message = "Company name must be at most 150 characters")
    private String name;

    @Size(max = 50, message = "Registration number must be at most 50 characters")
    private String registrationNumber;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Contact email must be a valid email address")
    @Size(max = 150, message = "Contact email must be at most 150 characters")
    private String contactEmail;

    @Pattern(regexp = "^[0-9+\\\\- ()]{0,20}$", message = "Contact phone must be a valid phone number")
    private String contactPhone;

    /** ID of the company owner. The user must exist and have ROLE_COMPANY. */
    @NotNull(message = "Company owner is required")
    private Long ownerUserId;
}