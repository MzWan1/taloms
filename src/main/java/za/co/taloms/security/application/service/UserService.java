package za.co.taloms.security.application.service;

import za.co.taloms.security.application.dto.*;
import za.co.taloms.common.PageResponse;
import java.util.List;

public interface UserService {
    UserResponse createUser(UserCreateRequest request);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    UserResponse findById(Long id);
    List<UserResponse> findAll();
    void deleteUser(Long id);
    void lockUser(Long id);
    void unlockUser(Long id);
    void changePassword(Long id, ChangePasswordRequest request);
    void initiatePasswordReset(PasswordResetRequest request);
    void confirmPasswordReset(PasswordResetConfirmRequest request);
    void resetPasswordByAdmin(Long userId);
}