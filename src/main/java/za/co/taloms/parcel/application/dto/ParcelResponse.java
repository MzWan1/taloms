package za.co.taloms.parcel.application.dto;

import lombok.*;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.parcel.domain.entity.ParcelType;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelResponse {

    private Long id;
    private String parcelNumber;
    private String standNumber;
    private ParcelType parcelType;
    private String parcelTypeDisplay;
    private String parcelTypeBadgeClass;
    private ParcelStatus status;
    private String statusDisplay;
    private String statusBadgeClass;
    private Double areaM2;
    private Double areaHectares;
    private Double centroidLat;
    private Double centroidLng;
    private Long villageId;
    private String villageName;
    private String authorityName;
    private Long ptoId;
    private String ptoNumber;
    private String ptoHolderName;
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<BoundaryPointDto> boundaries;
    private Integer boundaryCount;
}