package za.co.taloms.security.application.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Self-registration payload for external citizens/residents.
 * Registration NEVER honours a requested role — the system always assigns
 * ROLE_USER so that users not created by an administrator can never gain
 * privileged access.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Pattern(regexp = "\\d{13}", message = "ID number must be 13 digits")
    private String idNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}