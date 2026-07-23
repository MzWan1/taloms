package za.co.taloms.gis.application.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelGeoJsonResponse {
    private String type = "FeatureCollection";
    private List<ParcelFeature> features;
    private ParcelMetadata metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParcelFeature {
        private String type;
        private Map<String, Object> geometry;
        private ParcelProperties properties;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParcelProperties {
        private Long id;
        private String parcelNumber;
        private String standNumber;
        private String parcelType;
        private String parcelTypeDisplay;
        private String status;
        private String statusDisplay;
        private String villageName;
        private Double areaM2;
        private String ptoNumber;
        private String ptoHolderName;
        private String occupantTag;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParcelMetadata {
        private Long totalCount;
        private Long availableCount;
        private Long allocatedCount;
        private Double totalAreaM2;
        private String authorityName;
        private String villageName;
    }
}