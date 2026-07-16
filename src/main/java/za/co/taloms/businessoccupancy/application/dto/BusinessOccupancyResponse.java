package za.co.taloms.businessoccupancy.application.dto;

import lombok.*;
import za.co.taloms.businessoccupancy.domain.entity.BusinessStatus;
import za.co.taloms.businessoccupancy.domain.entity.BusinessType;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessOccupancyResponse {

    private Long id;
    private String businessName;
    private String registrationNumber;
    private BusinessType businessType;
    private String businessTypeDisplay;
    private String businessTypeBadgeClass;
    private String ownerName;
    private String ownerIdNumber;
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
    private String operatingHours;
    private Integer employeesCount;
    private BusinessStatus status;
    private String statusDisplay;
    private String statusBadgeClass;
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}