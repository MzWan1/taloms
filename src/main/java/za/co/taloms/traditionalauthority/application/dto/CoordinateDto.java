package za.co.taloms.traditionalauthority.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

/**
 * A single geographic coordinate (latitude / longitude) of a mapped boundary.
 * Accepts both {"latitude","longitude"} and the short {"lat","lng"} keys the
 * map editor produces.
 */
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CoordinateDto {
    @JsonAlias({"lat"})
    private Double latitude;
    @JsonAlias({"lng"})
    private Double longitude;
}
