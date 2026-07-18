package za.co.taloms.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvConfig {

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private String dbPort;

    @Value("${DB_NAME:taloms_db}")
    private String dbName;

    @Value("${DB_USERNAME:taloms_user}")
    private String dbUsername;

    @Value("${DB_PASSWORD:taloms_pass}")
    private String dbPassword;

    @Value("${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${JWT_EXPIRATION:28800000}")
    private Long jwtExpiration;

    @Value("${APP_BASE_URL:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${APP_UPLOAD_PATH:uploads}")
    private String uploadPath;

    @Value("${EMAIL_HOST:smtp.gmail.com}")
    private String emailHost;

    @Value("${EMAIL_PORT:587}")
    private Integer emailPort;

    @Value("${EMAIL_USERNAME:nkambulemm0@gmail.com}")
    private String emailUsername;

    @Value("${EMAIL_PASSWORD:wqctcwnfglglqskn}")
    private String emailPassword;

    @Value("${EMAIL_FROM:nkambulemm0@gmail.com}")
    private String emailFrom;

    // Getters
    public String getDbHost() { return dbHost; }
    public String getDbPort() { return dbPort; }
    public String getDbName() { return dbName; }
    public String getDbUsername() { return dbUsername; }
    public String getDbPassword() { return dbPassword; }
    public String getJwtSecret() { return jwtSecret; }
    public Long getJwtExpiration() { return jwtExpiration; }
    public String getAppBaseUrl() { return appBaseUrl; }
    public String getUploadPath() { return uploadPath; }
    public String getEmailHost() { return emailHost; }
    public Integer getEmailPort() { return emailPort; }
    public String getEmailUsername() { return emailUsername; }
    public String getEmailPassword() { return emailPassword; }
    public String getEmailFrom() { return emailFrom; }
}