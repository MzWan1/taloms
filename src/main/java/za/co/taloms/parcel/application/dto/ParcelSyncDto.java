package za.co.taloms.parcel.application.dto;

import lombok.*;
import za.co.taloms.parcel.domain.entity.CaptureMode;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.parcel.domain.entity.ParcelType;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelSyncDto {

    private Long id;
    private String parcelNumber;
    private String standNumber;
    private ParcelStatus status;
    private ParcelType parcelType;
    private Double areaM2;
    private Double areaHectares;
    private Double centroidLat;
    private Double centroidLng;
    private Double perimeterM;
    private Long villageId;
    private Long ptoId;
    private String notes;
    private CaptureMode captureMode;
    private String chiefName;
    private String headmanName;
    private Long version;
    private LocalDateTime updatedAt;
    private List<BoundaryPointDto> boundaries;
    private Boolean deleted;
}