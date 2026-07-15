package za.co.taloms.traditionalauthority.application.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TraditionalAuthorityResponse {
    private Long          id;
    private String        authorityName;
    private String        chiefName;
    private String        headmanName;
    private String        contactPhone;
    private String        contactEmail;
    private String        physicalAddress;
    private String        region;
    private Boolean       active;
    private long          villageCount;
    private String        createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}