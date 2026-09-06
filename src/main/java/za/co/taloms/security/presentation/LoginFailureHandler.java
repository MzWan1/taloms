package za.co.taloms.security.presentation;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import za.co.taloms.common.ApplicationConstants;
import za.co.taloms.security.domain.repository.UserRepositoryPort;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    public static final String ERROR_MESSAGE_KEY = "LOGIN_ERROR_MESSAGE";

    private final UserRepositoryPort userRepository;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String username = request.getParameter("username");
        String errorType = "invalid";

        if (username != null && !username.isBlank()) {
            var userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                var user = userOpt.get();

                if (!user.getEnabled()) {
                    // Account is deactivated
                    errorType = "deactivated";
                    log.debug("Login attempt on deactivated account: {}", username);
                } else if (user.getAccountLocked()) {
                    // Account is already locked
                    errorType = "locked";
                    log.debug("Login attempt on locked account: {}", username);
                } else {
                    // Track failed attempt
                    int attempts = user.getFailedLoginAttempts() + 1;
                    user.setFailedLoginAttempts(attempts);

                    if (attempts >= ApplicationConstants.MAX_FAILED_LOGIN_ATTEMPTS) {
                        user.setAccountLocked(true);
                        errorType = "locked";
                        log.warn("Account locked for user: {} after {} failed attempts",
                                user.getUsername(), attempts);
                    } else {
                        int remaining = ApplicationConstants.MAX_FAILED_LOGIN_ATTEMPTS - attempts;
                        errorType = "invalid_" + remaining;
                        log.debug("Failed login attempt {} for user: {}", attempts, username);
                    }

                    userRepository.save(user);
                }
            }
        }

        // Store error type in session for the login page to display
        request.getSession().setAttribute(ERROR_MESSAGE_KEY, errorType);
        super.setDefaultFailureUrl("/login?error=true");
        super.onAuthenticationFailure(request, response, exception);
    }
}
