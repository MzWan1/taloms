package za.co.taloms.security.application.service;

import za.co.taloms.security.application.dto.*;
import za.co.taloms.security.domain.entity.Role;
import java.util.List;

public interface UserService {

    // User CRUD
    UserResponse createUser(UserCreateRequest request);
    UserResponse register(RegisterRequest request);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    UserResponse findById(Long id);
    List<UserResponse> findAll();
    List<UserResponse> findUnassignedCompanyUsers();
    void deleteUser(Long id);
    void activateUser(Long id);

    // User status management
    void lockUser(Long id);
    void unlockUser(Long id);

    // Password management
    void changePassword(Long id, ChangePasswordRequest request);
    void initiatePasswordReset(PasswordResetRequest request);
    void confirmPasswordReset(PasswordResetConfirmRequest request);
    void resetPasswordByAdmin(Long userId);

    // Counts
    long countAll();
    long countActive();

    // Roles
    List<Role> findAllRoles();

    /**
     * IDs of the authorities a user belongs to (many-to-many chief ↔ authority).
     * Used by the admin user form and API responses.
     */
    java.util.Set<Long> getUserAuthorityIds(Long userId);
}

