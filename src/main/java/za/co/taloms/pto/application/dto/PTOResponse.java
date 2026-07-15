package za.co.taloms.pto.application.dto;

import lombok.*;
import za.co.taloms.pto.domain.entity.PTOPurpose;
import za.co.taloms.pto.domain.entity.PTOStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PTOResponse {
    private Long          id;
    private String        ptoNumber;
    private String        ptoHolderName;
    private String        idNumber;
    private String        contactPhone;
    private String        contactEmail;
    private PTOPurpose    purpose;
    private String        purposeDisplay;
    private PTOStatus     status;
    private String        statusDisplay;
    private String        statusBadgeClass;
    private LocalDate     issueDate;
    private LocalDate     expiryDate;
    private String        notes;
    private Long          villageId;
    private String        villageName;
    private Long          traditionalAuthorityId;
    private String        authorityName;
    private String        approvedBy;
    private LocalDateTime approvedAt;
    private String        revokedBy;
    private LocalDateTime revokedAt;
    private String        revokeReason;
    private String        createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}