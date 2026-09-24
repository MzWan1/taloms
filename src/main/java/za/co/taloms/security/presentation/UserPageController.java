package za.co.taloms.security.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.security.application.dto.*;
import za.co.taloms.security.application.service.UserService;
import za.co.taloms.security.domain.repository.UserRepositoryPort;
import za.co.taloms.security.infrastructure.repository.RoleJpaRepository;
import za.co.taloms.traditionalauthority.application.dto.TraditionalAuthorityResponse;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserPageController {

    private final UserService        userService;
    private final UserRepositoryPort userRepository;
    private final RoleJpaRepository  roleRepository;
    private final TraditionalAuthorityService authorityService;

    @GetMapping
    public String listUsers(Model model,
                            @RequestParam(required = false) String search,
                            @RequestParam(required = false) String roleFilter,
                            @RequestParam(required = false) String statusFilter,
                            @RequestParam(required = false, defaultValue = "1") Integer page) {
        var users = userService.findAll();
        if (search != null && !search.isBlank()) {
            String lower = search.toLowerCase();
            users = users.stream().filter(u -> (u.getFullName() != null && u.getFullName().toLowerCase().contains(lower)) ||
                                               (u.getUsername() != null && u.getUsername().toLowerCase().contains(lower)) ||
                                               (u.getEmail() != null && u.getEmail().toLowerCase().contains(lower))).toList();
        }
        if (roleFilter != null && !roleFilter.isBlank()) {
            String lower = roleFilter.toLowerCase();
            users = users.stream().filter(u -> u.getRoles() != null && u.getRoles().stream().anyMatch(r -> r.toLowerCase().contains(lower))).toList();
        }
        if (statusFilter != null && !statusFilter.isBlank()) {
            users = users.stream().filter(u -> (statusFilter.equalsIgnoreCase("active") && (u.getEnabled() != null && u.getEnabled()) && !(u.getAccountLocked() != null && u.getAccountLocked())) ||
                                               (statusFilter.equalsIgnoreCase("locked") && (u.getAccountLocked() != null && u.getAccountLocked())) ||
                                               (statusFilter.equalsIgnoreCase("inactive") && !(u.getEnabled() != null && u.getEnabled()))).toList();
        }
        var pageObj = za.co.taloms.common.pagination.PageRequestUtils.paginateList(users, page, 10);
        model.addAttribute("page", pageObj);
        model.addAttribute("users", pageObj.getContent());
        model.addAttribute("pageTitle", "User Management");
        model.addAttribute("currentPage","users");
        return "users/list";
    }

    @GetMapping("/create")
    public String createUserForm(Model model) {
        model.addAttribute("userForm",  new UserCreateRequest());
        model.addAttribute("roles",     roleRepository.findAll());
        model.addAttribute("authorities", authorityService.findAllActive());
        model.addAttribute("pageTitle", "Create User");
        model.addAttribute("currentPage","users");
        return "users/create";
    }

    @PostMapping("/create")
    public String createUser(
            @ModelAttribute("userForm") UserCreateRequest request,
            RedirectAttributes ra) {
        try {
            userService.createUser(request);
            ra.addFlashAttribute("successMessage",
                    "User '" + request.getUsername() + "' created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/users/create";
        }
        return "redirect:/users";
    }

    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        var user = userService.findById(id);
        var req  = UserUpdateRequest.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .roleName(user.getRoles().iterator().next())
                .traditionalAuthorityId(user.getTraditionalAuthorityId())
                .authorityIds(user.getAuthorityIds() == null
                        ? new java.util.ArrayList<>()
                        : new java.util.ArrayList<>(user.getAuthorityIds()))
                .idNumber(user.getIdNumber())
                .build();
        model.addAttribute("userForm",  req);
        model.addAttribute("userId",    id);
        model.addAttribute("user",      user);
        model.addAttribute("roles",     roleRepository.findAll());
        model.addAttribute("authorities", authorityService.findAllActive());
        model.addAttribute("pageTitle", "Edit User");
        model.addAttribute("currentPage","users");
        return "users/edit";
    }

    @PostMapping("/{id}/edit")
    public String editUser(
            @PathVariable Long id,
            @ModelAttribute("userForm") UserUpdateRequest request,
            RedirectAttributes ra) {
        try {
            userService.updateUser(id, request);
            ra.addFlashAttribute("successMessage", "User updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/users";
    }

    @PostMapping("/{id}/admin-reset")
    public String adminResetPassword(
            @PathVariable Long id,
            RedirectAttributes ra) {
        try {
            userService.resetPasswordByAdmin(id);
            ra.addFlashAttribute("successMessage",
                    "Password reset token generated. User must set a new password.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/users/" + id + "/edit";
    }
    
    @PostMapping("/{id}/deactivate")
    public String deactivateUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.deleteUser(id);
            ra.addFlashAttribute("successMessage", "User deactivated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/users";
    }

    @PostMapping("/{id}/activate")
    public String activateUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.activateUser(id);
            ra.addFlashAttribute("successMessage", "User account activated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/users";
    }

    @PostMapping("/{id}/lock")
    public String lockUser(@PathVariable Long id, RedirectAttributes ra) {
        userService.lockUser(id);
        ra.addFlashAttribute("successMessage", "User account locked.");
        return "redirect:/users";
    }

    @PostMapping("/{id}/unlock")
    public String unlockUser(@PathVariable Long id, RedirectAttributes ra) {
        userService.unlockUser(id);
        ra.addFlashAttribute("successMessage", "User account unlocked.");
        return "redirect:/users";
    }

    @GetMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public String changePasswordForm(Model model) {
        model.addAttribute("form",      new ChangePasswordRequest());
        model.addAttribute("pageTitle", "Change Password");
        model.addAttribute("currentPage","users");
        return "users/change-password";
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public String changePassword(
            @ModelAttribute("form") ChangePasswordRequest request,
            Principal principal,
            RedirectAttributes ra) {
        try {
            var user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            userService.changePassword(user.getId(), request);
            ra.addFlashAttribute("successMessage",
                    "Password changed successfully.");
            return "redirect:/dashboard";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/users/change-password";
        }
    }
}


