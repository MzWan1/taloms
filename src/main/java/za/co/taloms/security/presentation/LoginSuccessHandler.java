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
        super.onAuthenticationSuccess(request, response, authentication);
    }
}