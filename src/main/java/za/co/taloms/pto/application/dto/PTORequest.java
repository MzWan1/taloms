package za.co.taloms.pto.application.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import za.co.taloms.pto.domain.entity.PTOPurpose;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PTORequest {

    @NotBlank(message = "PTO holder name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String ptoHolderName;

    @NotBlank(message = "ID number is required")
    @Size(min = 13, max = 13, message = "SA ID number must be exactly 13 digits")
    @Pattern(regexp = "\\d{13}", message = "ID number must contain only digits")
    private String idNumber;

    @Pattern(regexp = "^(\\+27|0)[0-9]{9}$",
            message = "Enter a valid South African phone number")
    private String contactPhone;

    @Email(message = "Enter a valid email address")
    private String contactEmail;

    @NotNull(message = "Purpose is required")
    private String purpose;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "Issue date is required")
    private LocalDate issueDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;

    private String notes;

    @NotNull(message = "Village is required")
    private Long villageId;

    @NotNull(message = "Traditional Authority is required")
    private Long traditionalAuthorityId;

    @NotNull(message = "Parcel is required")
    private Long parcelId;

    @NotNull(message = "Stand Number is required")
    @Size(max = 50, message = "Stand number must not exceed 50 characters")
    private String standNumber;

    @NotNull(message = "Parcel Number is required")
    @Size(max = 50, message = "Parcel number must not exceed 50 characters")
    private String parcelNumber;

    @Size(max = 150, message = "Allocated by must not exceed 150 characters")
    private String allocatedBy;

    private LocalDate allocationDate;

    @DecimalMin(value = "0.0", inclusive = false)
    private Double standArea;

    @Size(max = 100, message = "Survey reference must not exceed 100 characters")
    private String surveyReference;

    private String boundaryDescription;

    @Size(max = 100, message = "Receipt reference must not exceed 100 characters")
    private String allocationFeeReceipt;

    @Size(max = 100, message = "TA recommendation reference must not exceed 100 characters")
    private String taRecommendationRef;

    private Boolean communityResolutionRequired;
}