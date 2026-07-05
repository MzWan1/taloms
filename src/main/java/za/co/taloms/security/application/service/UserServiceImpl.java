package za.co.taloms.security.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.common.*;
import za.co.taloms.security.application.dto.*;
import za.co.taloms.security.domain.entity.*;
import za.co.taloms.security.domain.repository.*;
import za.co.taloms.security.infrastructure.repository.RoleJpaRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepositoryPort              userRepository;
    private final PasswordResetTokenRepositoryPort tokenRepository;
    private final RoleJpaRepository               roleRepository;
    private final PasswordEncoder                 passwordEncoder;

    @Override
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateRecordException("User", "username",
                    request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateRecordException("User", "email",
                    request.getEmail());
        }

        var role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role not found: " + request.getRoleName()));

        var user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>(Set.of(role)))
                .build();

        var saved = userRepository.save(user);
        log.info("Created user: {}", saved.getUsername());
        return toResponse(saved);
    }

    @Override
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        var user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", id));

        var role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role not found: " + request.getRoleName()));

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRoles(new HashSet<>(Set.of(role)));

        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUser(Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", id));
        user.setEnabled(false);
        userRepository.save(user);
        log.info("Deactivated user: {}", user.getUsername());
    }

    @Override
    public void lockUser(Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", id));
        user.setAccountLocked(true);
        userRepository.save(user);
    }

    @Override
    public void unlockUser(Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", id));
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
    }

    @Override
    public void changePassword(Long id, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessValidationException(
                    "New password and confirm password do not match");
        }

        var user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", id));

        if (!passwordEncoder.matches(
                request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessValidationException(
                    "Current password is incorrect");
        }

        user.setPasswordHash(
                passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getUsername());
    }

    @Override
    public void initiatePasswordReset(PasswordResetRequest request) {
        var userOpt = userRepository.findByEmail(request.getEmail());

        // Always return success even if email not found (security best practice)
        if (userOpt.isEmpty()) {
            log.warn("Password reset requested for unknown email: {}",
                    request.getEmail());
            return;
        }

        var user = userOpt.get();
        tokenRepository.deleteByUserId(user.getId());

        var token = PasswordResetToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        var saved = tokenRepository.save(token);
        log.info("Password reset token generated for: {} — token: {}",
                user.getEmail(), saved.getToken());
        // Phase 2: send email with reset link
    }

    @Override
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessValidationException(
                    "Passwords do not match");
        }

        var token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BusinessValidationException(
                        "Invalid or expired reset token"));

        if (!token.isValid()) {
            throw new BusinessValidationException(
                    "Reset token has expired or already been used");
        }

        var user = token.getUser();
        user.setPasswordHash(
                passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
        log.info("Password reset completed for: {}", user.getEmail());
    }

    @Override
    public void resetPasswordByAdmin(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", userId));

        // Admin reset generates a token — user must set new password on login
        tokenRepository.deleteByUserId(userId);
        var token = PasswordResetToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        tokenRepository.save(token);
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        log.info("Admin password reset initiated for: {}",
                user.getUsername());
    }

    private UserResponse toResponse(User user) {
        var roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .enabled(user.getEnabled())
                .accountLocked(user.getAccountLocked())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .roles(roles)
                .build();
    }
}