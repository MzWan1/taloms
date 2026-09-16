package za.co.taloms.security.presentation;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import za.co.taloms.security.application.service.LoginAuditService;
import za.co.taloms.security.domain.repository.UserRepositoryPort;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final LoginAuditService loginAuditService;
    private final UserRepositoryPort userRepository;

        @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        String username = authentication.getName();

        // Reset failed login attempts on successful login
        // Note: This does NOT unlock the account - only admin can unlock
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);
                log.debug("Reset failed login attempts for user: {}", username);
            }
        });

        loginAuditService.recordSuccessfulLogin(username);

        // Preserve saved-request behaviour (e.g. user clicked a link to /portal
        // while unauthenticated → forwarded to /login → should return there).
        // When there is no saved request, redirect by role:
        //   ROLE_ADMIN/ROLE_CHIEF/ROLE_HEADSMAN    → /dashboard
        //   ROLE_COMPANY                            → /dashboard (company self-service)
        //   ROLE_USER-only                         → /portal   (self-service resident portal)
        String targetUrl = determineTargetUrl(request, response, authentication);
        if (targetUrl == null || targetUrl.isEmpty() || "/".equals(targetUrl)) {
            boolean isAdminOrStaffOrCompany = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                            || "ROLE_CHIEF".equals(a.getAuthority())
                            || "ROLE_HEADSMAN".equals(a.getAuthority())
                            || "ROLE_COMPANY".equals(a.getAuthority()));
            targetUrl = isAdminOrStaffOrCompany ? "/dashboard" : "/portal";
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}