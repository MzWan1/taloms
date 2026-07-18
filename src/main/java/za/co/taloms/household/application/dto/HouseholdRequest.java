package za.co.taloms.household.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdRequest {

    @NotBlank(message = "Household head name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String householdHeadName;

    @NotBlank(message = "ID number is required")
    @Size(min = 13, max = 13, message = "SA ID number must be exactly 13 digits")
    @Pattern(regexp = "\\d{13}", message = "ID number must contain only digits")
    private String householdHeadIdNumber;

    @Pattern(regexp = "^(\\+27|0)[0-9]{9}$",
            message = "Enter a valid South African phone number")
    private String contactPhone;

    @jakarta.validation.constraints.Email(message = "Enter a valid email address")
    private String contactEmail;

    @NotNull(message = "Parcel is required")
    private Long parcelId;

    private Long ptoId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registrationDate;

    private String notes;
}