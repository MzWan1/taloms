package za.co.taloms.security.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom {@link AuthenticationEntryPoint} that returns a structured JSON
 * response for API requests and redirects to the login page for browser (MVC)
 * requests.
 */
@Slf4j
@Component
public class TalomsAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                          HttpServletResponse response,
                          AuthenticationException authException) throws IOException {

        String requestUri = request.getRequestURI();
        boolean isApi = requestUri.startsWith("/api");

        if (isApi) {
            log.warn("Authentication required for API request: {}", requestUri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            String json = """
                {"success":false,"message":"Authentication required. Please log in.","timestamp":"%s"}
                """.formatted(java.time.LocalDateTime.now().toString());
            response.getWriter().write(json);
        } else {
            log.warn("Authentication required for page request: {}", requestUri);
            response.sendRedirect(request.getContextPath() + "/login?error=true");
        }
    }
}
