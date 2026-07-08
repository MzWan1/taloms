package za.co.taloms.security.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.security.application.dto.*;
import za.co.taloms.security.application.service.UserService;
import za.co.taloms.security.infrastructure.repository.RoleJpaRepository;
import java.security.Principal;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class UserPageController {

    private final UserService        userService;
    private final RoleJpaRepository  roleRepository;

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users",     userService.findAll());
        model.addAttribute("pageTitle", "User Management");
        model.addAttribute("currentPage","users");
        return "users/list";
    }

    @GetMapping("/create")
    public String createUserForm(Model model) {
        model.addAttribute("userForm",  new UserCreateRequest());
        model.addAttribute("roles",     roleRepository.findAll());
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
                .build();
        model.addAttribute("userForm",  req);
        model.addAttribute("userId",    id);
        model.addAttribute("user",      user);
        model.addAttribute("roles",     roleRepository.findAll());
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
            var user = userService.findAll().stream()
                    .filter(u -> u.getUsername()
                            .equals(principal.getName()))
                    .findFirst()
                    .orElseThrow();
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