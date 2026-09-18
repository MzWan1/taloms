package za.co.taloms.businessoccupancy.application.dto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.*;
import za.co.taloms.common.MaskedIdNumberSerializer;
import za.co.taloms.common.MaskedLongSerializer;
import za.co.taloms.businessoccupancy.domain.entity.BusinessStatus;
import za.co.taloms.businessoccupancy.domain.entity.BusinessType;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessOccupancyResponse {

    @JsonSerialize(using = MaskedLongSerializer.class)
    private Long id;
    private String businessName;
    private String registrationNumber;
    private BusinessType businessType;
    private String businessTypeDisplay;
    private String businessTypeBadgeClass;
    private String ownerName;
    @JsonSerialize(using = MaskedIdNumberSerializer.class)
    private String ownerIdNumber;
    private String contactPhone;
    private String contactEmail;
    @JsonSerialize(using = MaskedLongSerializer.class)
    private Long parcelId;
    private String standNumber;
    private String parcelNumber;
    private String villageName;
    private String authorityName;
    @JsonSerialize(using = MaskedLongSerializer.class)
    private Long ptoId;
    private String ptoNumber;
    private String ptoHolderName;
    @JsonSerialize(using = MaskedLongSerializer.class)
    private Long householdId;
    private String householdHeadName;
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

