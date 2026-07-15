package za.co.taloms.traditionalauthority.application.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VillageResponse {
    private Long          id;
    private String        villageName;
    private String        region;
    private String        headmanName;
    private String        description;
    private Boolean       active;
    private Long          traditionalAuthorityId;
    private String        authorityName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}