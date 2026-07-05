package za.co.taloms.security.application.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserResponse {
    private Long          id;
    private String        username;
    private String        email;
    private String        fullName;
    private Boolean       enabled;
    private Boolean       accountLocked;
    private Integer       failedLoginAttempts;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private Set<String>   roles;
}