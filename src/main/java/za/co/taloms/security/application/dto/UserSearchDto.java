package za.co.taloms.security.application.dto;

import lombok.*;

/**
 * Lightweight DTO for user search results.
 * Does NOT expose username for privacy.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserSearchDto {
    private Long    id;
    private String  fullName;
    private String  email;
    private String  roleName;
}
