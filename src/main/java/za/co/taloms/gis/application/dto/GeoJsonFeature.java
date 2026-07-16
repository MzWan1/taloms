package za.co.taloms.gis.application.dto;

import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoJsonFeature {
    @Builder.Default
    private String type = "Feature";
    private Map<String, Object> geometry;
    private Map<String, Object> properties;
}