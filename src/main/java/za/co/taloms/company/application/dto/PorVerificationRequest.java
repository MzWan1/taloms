package za.co.taloms.company.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * External request to verify whether a resident has a valid proof of residence.
 *
 * The ID number is the ONLY input: the API never accepts a resident id, a name
 * or any other search field, so unrestricted resident lookup is impossible.
 * Validation failures never echo the supplied value back to the caller.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PorVerificationRequest {

    @NotBlank(message = "ID number is required")
    @Size(min = 13, max = 13, message = "ID number must be exactly 13 digits")
    @Pattern(regexp = "\\d{13}", message = "ID number must contain exactly 13 digits")
    private String idNumber;
}