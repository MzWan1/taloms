package za.co.taloms.security.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.security.application.dto.RegisterRequest;
import za.co.taloms.security.application.service.UserService;

@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegisterController {

    private final UserService userService;

    @GetMapping
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping
    public String register(@ModelAttribute("registerForm") RegisterRequest request,
                           RedirectAttributes ra) {
        try {
            userService.register(request);
            ra.addFlashAttribute("successMessage",
                    "Registration successful. Please sign in.");
            return "redirect:/login?registered=true";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register";
        }
    }
}