package za.co.taloms.pto.application.dto;

import lombok.*;
import za.co.taloms.pto.domain.entity.PTOPurpose;
import za.co.taloms.pto.domain.entity.PTOStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PTOSyncDto {

    private Long id;
    private String ptoNumber;
    private String ptoHolderName;
    private String idNumber;
    private String contactPhone;
    private String contactEmail;
    private PTOPurpose purpose;
    private PTOStatus status;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String notes;
    private Long villageId;
    private Long traditionalAuthorityId;
    private Long parcelId;

    // Approval
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String approvalNotes;

    // Revocation
    private String revokedBy;
    private LocalDateTime revokedAt;
    private String revokeReason;

    // Allocation metadata
    private String allocatedBy;
    private LocalDate allocationDate;
    private Double standArea;
    private String surveyReference;
    private String boundaryDescription;
    private String allocationFeeReceipt;
    private String taRecommendationRef;
    private Boolean communityResolutionRequired;

    // Audit
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Boolean deleted;
}
