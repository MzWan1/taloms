package za.co.taloms.household.application.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdResponse {

    private Long id;
    private String householdHeadName;
    private String householdHeadIdNumber;
    private String contactPhone;
    private String contactEmail;
    private Long parcelId;
    private String standNumber;
    private String parcelNumber;
    private String villageName;
    private String authorityName;
    private Long ptoId;
    private String ptoNumber;
    private String ptoHolderName;
    private LocalDate registrationDate;
    private Boolean active;
    private String statusDisplay;
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}