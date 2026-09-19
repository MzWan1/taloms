package za.co.taloms.security.application.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserUpdateRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Role is required")
    private String roleName;

    /** Legacy single-authority link (headsman). */
    private Long traditionalAuthorityId;

    /**
     * Authorities a CHIEF user belongs to (many-to-many). When the role is
     * ROLE_CHIEF this list REPLACES the chief's existing authority links.
     */
    private java.util.List<Long> authorityIds;

    @Pattern(regexp = "\\d{13}", message = "ID number must be 13 digits (for proof-of-residence ownership)")
    private String idNumber;
}

