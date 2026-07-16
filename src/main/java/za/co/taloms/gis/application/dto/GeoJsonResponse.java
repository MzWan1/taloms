package za.co.taloms.gis.application.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoJsonResponse {
    private String type = "FeatureCollection";
    private List<GeoJsonFeature> features;
    private Map<String, Object> metadata;
}