package za.co.taloms.security.application.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import za.co.taloms.common.MaskedIdNumberSerializer;
import za.co.taloms.common.MaskedLongSerializer;

import java.time.LocalDateTime;
import java.util.Set;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserResponse {
    @JsonSerialize(using = MaskedLongSerializer.class)
    private Long          id;
    private String        username;
    private String        email;
    private String        fullName;
    private Boolean       enabled;
    private Boolean       accountLocked;
    private Integer       failedLoginAttempts;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    @JsonSerialize(using = MaskedLongSerializer.class)
    private Long          traditionalAuthorityId;
    /** Authorities a CHIEF user belongs to (many-to-many chief ↔ authority). */
    private Set<Long>     authorityIds;
    @JsonSerialize(using = MaskedIdNumberSerializer.class)
    private String        idNumber;
    private Set<String>   roles;
}

