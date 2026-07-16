package za.co.taloms.document.application.dto;

import lombok.*;
import za.co.taloms.document.domain.entity.DocumentType;
import za.co.taloms.document.domain.entity.EntityType;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private Long id;
    private String originalFilename;
    private String storedFilename;
    private String contentType;
    private Long fileSize;
    private String fileSizeDisplay;
    private DocumentType documentType;
    private String documentTypeDisplay;
    private String documentTypeBadgeClass;
    private String documentTypeIcon;
    private EntityType relatedEntityType;
    private String entityTypeDisplay;
    private Long relatedEntityId;
    private String description;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
    private Boolean active;
    private String statusDisplay;
    private Integer version;
    private String checksum;
    private String downloadUrl;
    private String notes;
}