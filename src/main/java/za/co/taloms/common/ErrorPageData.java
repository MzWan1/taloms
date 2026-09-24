package za.co.taloms.common;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility for mapping HTTP status codes to error page metadata
 * such as titles, icons, descriptions, and the appropriate go-home URL.
 */
public final class ErrorPageData {

    private ErrorPageData() { }

    public static String getTitle(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Authentication Required";
            case 403 -> "Access Denied";
            case 404 -> "Page Not Found";
            case 405 -> "Method Not Allowed";
            case 408 -> "Request Timeout";
            case 409 -> "Conflict";
            case 413 -> "File Too Large";
            case 415 -> "Unsupported Media Type";
            case 422 -> "Validation Error";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default  -> "Something Went Wrong";
        };
    }

    public static String getIcon(int status) {
        return "bi " + switch (status) {
            case 400 -> "bi-exclamation-octagon";
            case 401 -> "bi-lock";
            case 403 -> "bi-shield-lock";
            case 404 -> "bi-emoji-display-no";
            case 405 -> "bi-hand-index";
            case 408 -> "bi-clock";
            case 409 -> "bi-exclamation-triangle";
            case 413 -> "bi-file-earmark-x";
            case 415 -> "bi-file-earmark-ruled";
            case 422 -> "bi-exclamation-octagon";
            case 429 -> "bi-speedometer2";
            case 500 -> "bi-server";
            case 502 -> "bi-router";
            case 503 -> "bi-power";
            case 504 -> "bi-clock";
            default  -> "bi-exclamation-triangle";
        };
    }

    public static String getDescription(int status) {
        return switch (status) {
            case 400 -> "The server could not understand your request. Please check your input and try again.";
            case 401 -> "You need to log in to access this page. Please sign in to continue.";
            case 403 -> "You do not have permission to view this page. If you believe this is an error, contact your administrator.";
            case 404 -> "The page you are looking for does not exist or has been moved. Please check the URL or return to the dashboard.";
            case 405 -> "The HTTP method you used is not allowed for this resource. Please use the correct method.";
            case 408 -> "The server timed out waiting for your request. Please try again.";
            case 409 -> "There was a conflict with the current state of the requested resource. Please refresh and try again.";
            case 413 -> "The file you tried to upload exceeds the maximum allowed size. Please upload a smaller file.";
            case 415 -> "The media type of the request is not supported by this endpoint.";
            case 422 -> "The request was well-formed but contained semantic errors. Please review your input and try again.";
            case 429 -> "You have made too many requests. Please wait a moment and try again.";
            case 500 -> "Something went wrong on our end. Our team has been notified and is working on fixing the issue.";
            case 502 -> "The server received an invalid response from an upstream server. Please try again later.";
            case 503 -> "The service is temporarily unavailable. Please try again in a few minutes.";
            case 504 -> "The server timed out waiting for a response from an upstream server. Please try again later.";
            default  -> "An unexpected error occurred. Our team has been notified and is working on fixing the issue.";
        };
    }

    public static String getHomeUrl(int status, boolean authenticated) {
        return switch (status) {
            case 401, 403, 502, 503 -> "/login";
            default -> authenticated ? "/dashboard" : "/login";
        };
    }

    public static String getColor(int status) {
        return switch (status) {
            case 400, 408, 409, 413, 422, 429, 503 -> "text-warning";
            case 401                              -> "text-navy";
                        case 403, 500, 502, 504               -> "text-danger";
            case 404, 405, 415                    -> "text-navy";
            default                               -> "text-navy";
        };
    }

    public static boolean isUserAuthenticated(HttpServletRequest request) {
        return request.getUserPrincipal() != null;
    }
}
