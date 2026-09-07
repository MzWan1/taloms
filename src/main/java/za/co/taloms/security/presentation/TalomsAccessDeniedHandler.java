package za.co.taloms.security.presentation;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom {@link AccessDeniedHandler} that returns a structured JSON response
 * for API requests and forwards to the TALOMS error page for browser (MVC)
 * requests.
 */
@Slf4j
@Component
public class TalomsAccessDeniedHandler implements AccessDeniedHandler {

    @Override
        public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        String requestUri = request.getRequestURI();
        boolean isApi = requestUri.startsWith("/api");

        if (isApi) {
            log.warn("Access denied for API request: {}", requestUri);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            String json = """
                {"success":false,"message":"You do not have permission to perform this action","timestamp":"%s"}
                """.formatted(java.time.LocalDateTime.now().toString());
            response.getWriter().write(json);
        } else {
            log.warn("Access denied for page request: {}", requestUri);
            // Forward to the error endpoint which will render the error page
            request.setAttribute("javax.servlet.error.status_code", HttpServletResponse.SC_FORBIDDEN);
            request.setAttribute("javax.servlet.error.exception", accessDeniedException);
            request.setAttribute("javax.servlet.error.message", accessDeniedException.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.getRequestDispatcher("/error").forward(request, response);
        }
    }
}
