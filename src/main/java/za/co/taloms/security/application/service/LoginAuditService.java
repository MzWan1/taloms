package za.co.taloms.security.application.service;

public interface LoginAuditService {
    void recordSuccessfulLogin(String username);
}