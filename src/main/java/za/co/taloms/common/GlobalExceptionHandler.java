package za.co.taloms.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String API_PREFIX = "/api";

    private boolean isApiRequest(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith(API_PREFIX);
    }

    /** Correlation ID attached by ApiProtectionFilter (company API sets its own). */
    private String requestId(HttpServletRequest request) {
        Object id = request.getAttribute(za.co.taloms.common.resiliency.ApiProtectionFilter.REQUEST_ID_ATTRIBUTE);
        return id != null ? id.toString() : "-";
    }

    private String getErrorView(int statusCode) {
        return switch (statusCode) {
            case 400, 401, 403, 404, 405, 408, 409, 415, 422, 429,
                     500, 502, 503, 504 -> "error/" + statusCode;
            default                                     -> "error/error";
        };
    }

    private ResponseEntity<ApiResponse<Void>> jsonError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }

    private void setMvcErrorAttributes(Model model, HttpServletRequest request,
                                       HttpStatus status, Exception ex) {
        model.addAttribute("status", status.value());
        model.addAttribute("error", status.getReasonPhrase());
        model.addAttribute("message",
                ex.getMessage() != null ? ex.getMessage() : status.getReasonPhrase());
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("timestamp",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        boolean authenticated = ErrorPageData.isUserAuthenticated(request);
        model.addAttribute("errorTitle", ErrorPageData.getTitle(status.value()));
        model.addAttribute("errorIcon", ErrorPageData.getIcon(status.value()));
        model.addAttribute("errorDescription", ErrorPageData.getDescription(status.value()));
        model.addAttribute("homeUrl", ErrorPageData.getHomeUrl(status.value(), authenticated));
        model.addAttribute("errorColor", ErrorPageData.getColor(status.value()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex,
                                 HttpServletRequest request, Model model) {
        if (isApiRequest(request)) {
            log.warn("Resource not found: {}", ex.getMessage());
            return jsonError(HttpStatus.NOT_FOUND, ex.getMessage());
        }
        setMvcErrorAttributes(model, request, HttpStatus.NOT_FOUND, ex);
        return getErrorView(HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(BusinessValidationException.class)
    public Object handleBusinessValidation(BusinessValidationException ex,
                                           HttpServletRequest request, Model model) {
        if (isApiRequest(request)) {
            log.warn("Business validation failed: {}", ex.getMessage());
            return jsonError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        }
        setMvcErrorAttributes(model, request, HttpStatus.UNPROCESSABLE_ENTITY, ex);
        return getErrorView(HttpStatus.UNPROCESSABLE_ENTITY.value());
    }

    @ExceptionHandler(DuplicateRecordException.class)
    public Object handleDuplicate(DuplicateRecordException ex,
                                  HttpServletRequest request, Model model) {
        if (isApiRequest(request)) {
            log.warn("Duplicate record: {} [requestId={}]", ex.getMessage(), requestId(request));
            return jsonError(HttpStatus.CONFLICT, ex.getMessage());
        }
        setMvcErrorAttributes(model, request, HttpStatus.CONFLICT, ex);
        return getErrorView(HttpStatus.CONFLICT.value());
    }

    /**
     * Race-safe duplicate protection: when two identical submissions pass a
     * check-then-insert guard simultaneously, the database's unique/partial
     * unique constraint fails the loser here. The client gets the same 409 as
     * an expected duplicate — never a raw constraint or SQL error message.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrity(DataIntegrityViolationException ex,
                                      HttpServletRequest request, Model model) {
        log.warn("Data integrity violation on {} [requestId={}]: {}",
                request.getRequestURI(), requestId(request), ex.getMostSpecificCause().getClass().getSimpleName());
        if (isApiRequest(request)) {
            return jsonError(HttpStatus.CONFLICT,
                    "This record already exists or was modified by another user. Please refresh and try again.");
        }
        setMvcErrorAttributes(model, request, HttpStatus.CONFLICT,
                new DuplicateRecordException("Record"));
        return getErrorView(HttpStatus.CONFLICT.value());
    }

    /** Multipart body exceeded the configured maximum (before any storage). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleMaxUpload(MaxUploadSizeExceededException ex,
                                  HttpServletRequest request, Model model) {
        log.warn("Upload rejected: body exceeds maximum size [requestId={}]",
                requestId(request));
        if (isApiRequest(request)) {
            return jsonError(HttpStatus.PAYLOAD_TOO_LARGE,
                    "File exceeds the maximum allowed size. The limit is "
                            + (za.co.taloms.common.ApplicationConstants.MAX_FILE_SIZE_BYTES / (1024 * 1024)) + "MB.");
        }
        setMvcErrorAttributes(model, request, HttpStatus.PAYLOAD_TOO_LARGE, ex);
        return getErrorView(HttpStatus.PAYLOAD_TOO_LARGE.value());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidationErrors(MethodArgumentNotValidException ex,
                                         HttpServletRequest request, Model model) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Map<String, String>>builder()
                            .success(false)
                            .message("Validation failed")
                            .data(errors)
                            .build());
        }

        List<Map<String, String>> errorList = errors.entrySet().stream()
                .map(e -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("field", e.getKey());
                    m.put("defaultMessage", e.getValue());
                    return m;
                })
                .toList();
        model.addAttribute("errors", errorList);
        setMvcErrorAttributes(model, request, HttpStatus.BAD_REQUEST, ex);
        return getErrorView(HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public Object handleBadCredentials(BadCredentialsException ex,
                                       HttpServletRequest request, Model model) {
        if (isApiRequest(request)) {
            return jsonError(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        setMvcErrorAttributes(model, request, HttpStatus.UNAUTHORIZED, ex);
        return getErrorView(HttpStatus.UNAUTHORIZED.value());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex,
                                     HttpServletRequest request, Model model) {
        if (isApiRequest(request)) {
            return jsonError(HttpStatus.FORBIDDEN,
                    "You do not have permission to perform this action");
        }
        setMvcErrorAttributes(model, request, HttpStatus.FORBIDDEN, ex);
        return getErrorView(HttpStatus.FORBIDDEN.value());
    }

    @ExceptionHandler(SecurityException.class)
    public Object handleScopeViolation(SecurityException ex,
                                       HttpServletRequest request, Model model) {
        if (isApiRequest(request)) {
            log.warn("Data-scope access denied: {}", ex.getMessage());
            return jsonError(HttpStatus.FORBIDDEN, ex.getMessage());
        }
        setMvcErrorAttributes(model, request, HttpStatus.FORBIDDEN, ex);
        return getErrorView(HttpStatus.FORBIDDEN.value());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Object handleResponseStatus(ResponseStatusException ex,
                                       HttpServletRequest request, Model model) {
        int statusCode = ex.getStatusCode().value();
        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();

        if (isApiRequest(request)) {
            return jsonError(status, message);
        }
        setMvcErrorAttributes(model, request, status,
                new Exception(message, ex.getCause()));
        return getErrorView(statusCode);
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneral(Exception ex,
                                HttpServletRequest request, Model model) {
        log.error("Unexpected error on {}: {}", request.getRequestURI(),
                ex.getMessage(), ex);

        if (isApiRequest(request)) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred");
        }

        setMvcErrorAttributes(model, request, HttpStatus.INTERNAL_SERVER_ERROR, ex);
        return getErrorView(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
