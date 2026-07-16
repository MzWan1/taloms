package za.co.taloms.resident.application.dto;

import lombok.*;
import za.co.taloms.resident.domain.entity.Gender;
import za.co.taloms.resident.domain.entity.RelationshipType;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentResponse {

    private Long id;
    private String fullName;
    private String idNumber;
    private LocalDate dateOfBirth;
    private Integer age;
    private Gender gender;
    private String genderDisplay;
    private RelationshipType relationshipType;
    private String relationshipDisplay;
    private String relationshipBadgeClass;
    private String occupation;
    private String contactPhone;
    private String contactEmail;
    private Long householdId;
    private String householdHeadName;
    private String standNumber;
    private String villageName;
    private String authorityName;
    private Boolean active;
    private String statusDisplay;
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}