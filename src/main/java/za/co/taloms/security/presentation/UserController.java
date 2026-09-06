package za.co.taloms.security.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.security.application.dto.*;
import za.co.taloms.security.application.service.UserService;
import za.co.taloms.security.domain.entity.User;
import za.co.taloms.security.domain.repository.UserRepositoryPort;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepositoryPort userRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        userService.createUser(request),
                        "User created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(
                ApiResponse.success(userService.findAll(),
                        "Users retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(userService.findById(id),
                        "User retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        userService.updateUser(id, request),
                        "User updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(
                ApiResponse.success(null, "User deactivated successfully"));
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> lockUser(
            @PathVariable Long id) {
        userService.lockUser(id);
        return ResponseEntity.ok(
                ApiResponse.success(null, "User locked successfully"));
    }

    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlockUser(
            @PathVariable Long id) {
        userService.unlockUser(id);
        return ResponseEntity.ok(
                ApiResponse.success(null, "User unlocked successfully"));
    }

    @PatchMapping("/{id}/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Password changed successfully"));
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> adminResetPassword(
            @PathVariable Long id) {
        userService.resetPasswordByAdmin(id);
        return ResponseEntity.ok(
                ApiResponse.success(null,
                        "Password reset initiated successfully"));
    }

        @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    public ResponseEntity<ApiResponse<List<UserSearchDto>>> searchUsers(
            @RequestParam String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long authorityId) {
        if (q == null || q.trim().length() < 3) {
            return ResponseEntity.ok(ApiResponse.success(
                    List.of(), "Enter at least 3 characters"));
        }
        List<User> users = (authorityId != null)
                ? userRepository.searchByNameOrEmailAndAuthorityScope(q.trim(), authorityId)
                : userRepository.searchByNameOrEmail(q.trim());
        String message = (authorityId != null && users.isEmpty())
                ? "No eligible headsmen found for this authority"
                : "Search completed";
        List<UserSearchDto> results = users.stream()
                .filter(u -> role == null || role.isBlank() ||
                        (!u.getRoles().isEmpty() && u.getRoles().stream()
                                .anyMatch(r -> r.getName().equals(role))))
                .map(u -> UserSearchDto.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .roleName(u.getRoles().isEmpty() ? null :
                                u.getRoles().iterator().next().getName())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(results, message));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody PasswordResetRequest request) {
        userService.initiatePasswordReset(request);
        return ResponseEntity.ok(
                ApiResponse.success(null,
                        "If that email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        userService.confirmPasswordReset(request);
        return ResponseEntity.ok(
                ApiResponse.success(null,
                        "Password reset successfully"));
    }
}


