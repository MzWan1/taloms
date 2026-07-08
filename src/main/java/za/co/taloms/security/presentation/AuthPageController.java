package za.co.taloms.security.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.security.application.dto.PasswordResetRequest;
import za.co.taloms.security.application.service.UserService;

@Controller
@RequiredArgsConstructor
public class AuthPageController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam("email") String email,
            RedirectAttributes ra) {
        try {
            userService.initiatePasswordReset(
                    PasswordResetRequest.builder()
                            .email(email)
                            .build());
        } catch (Exception e) {
            // Silently ignore — never reveal if email exists
        }
        ra.addFlashAttribute("successMessage",
                "If that email is registered, a reset link has been sent.");
        return "redirect:/login?reset=true";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(
            @RequestParam(value = "token", required = false) String token,
            Model model) {
        if (token == null || token.isBlank()) {
            return "redirect:/forgot-password";
        }
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam("token")           String token,
            @RequestParam("newPassword")     String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes ra) {
        try {
            userService.confirmPasswordReset(
                    za.co.taloms.security.application.dto
                            .PasswordResetConfirmRequest.builder()
                            .token(token)
                            .newPassword(newPassword)
                            .confirmPassword(confirmPassword)
                            .build());
            ra.addFlashAttribute("successMessage",
                    "Password reset successfully. Please log in.");
            return "redirect:/login";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/reset-password?token=" + token;
        }
    }
}