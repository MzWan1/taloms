package za.co.taloms.common;

public final class ApplicationConstants {

    private ApplicationConstants() {}

    // PTO
    public static final String PTO_NUMBER_PREFIX = "PTO";
    public static final int PTO_NUMBER_PADDING   = 5;

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE     = 100;

    // File upload
    public static final long MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024; // 20MB
    public static final String[] ALLOWED_FILE_TYPES = {
            "application/pdf",
            "image/jpeg",
            "image/png",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    };

    // Audit actions
    public static final String AUDIT_CREATE  = "CREATE";
    public static final String AUDIT_UPDATE  = "UPDATE";
    public static final String AUDIT_DELETE  = "DELETE";
    public static final String AUDIT_APPROVE = "APPROVE";
    public static final String AUDIT_REVOKE  = "REVOKE";
    public static final String AUDIT_LOGIN   = "LOGIN";

    // Security
    public static final int    MAX_FAILED_LOGIN_ATTEMPTS = 5;
    public static final long   JWT_EXPIRY_HOURS          = 8;
    public static final String TOKEN_PREFIX              = "Bearer ";
    public static final String AUTH_HEADER               = "Authorization";

    // Date formats
    public static final String DATE_FORMAT     = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // South Africa coordinate bounds
    public static final double SA_LAT_MIN = -35.0;
    public static final double SA_LAT_MAX = -22.0;
    public static final double SA_LON_MIN =  16.0;
    public static final double SA_LON_MAX =  33.0;
}