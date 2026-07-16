package za.co.taloms.businessoccupancy.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessOccupancyRequest {

    @NotBlank(message = "Business name is required")
    @Size(max = 200, message = "Business name must not exceed 200 characters")
    private String businessName;

    @Size(max = 50, message = "Registration number must not exceed 50 characters")
    private String registrationNumber;

    @NotBlank(message = "Business type is required")
    private String businessType;

    @NotBlank(message = "Owner name is required")
    @Size(max = 150, message = "Owner name must not exceed 150 characters")
    private String ownerName;

    @NotBlank(message = "Owner ID number is required")
    @Size(min = 13, max = 13, message = "SA ID number must be exactly 13 digits")
    @Pattern(regexp = "\\d{13}", message = "ID number must contain only digits")
    private String ownerIdNumber;

    @Pattern(regexp = "^(\\+27|0)[0-9]{9}$",
            message = "Enter a valid South African phone number")
    private String contactPhone;

    @jakarta.validation.constraints.Email(message = "Enter a valid email address")
    private String contactEmail;

    @NotNull(message = "Parcel is required")
    private Long parcelId;

    @NotNull(message = "PTO is required")
    private Long ptoId;

    @Size(max = 200, message = "Operating hours must not exceed 200 characters")
    private String operatingHours;

    private Integer employeesCount;

    private String notes;
}