package za.co.taloms.security.application.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetToken, String fullName);
}