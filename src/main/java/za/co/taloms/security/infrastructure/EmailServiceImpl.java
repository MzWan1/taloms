package za.co.taloms.security.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import za.co.taloms.security.application.service.EmailService;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${taloms.app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username:noreply@taloms.co.za}")
    private String fromEmail;

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail,
                                       String resetToken,
                                       String fullName) {
        try {
            String resetLink = baseUrl + "/reset-password?token=" + resetToken;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("TALOMS — Password Reset Request");
            message.setText(
                    "Dear " + fullName + ",\n\n" +
                            "You requested a password reset for your TALOMS account.\n\n" +
                            "Click the link below to reset your password:\n" +
                            resetLink + "\n\n" +
                            "This link expires in 1 hour.\n\n" +
                            "If you did not request this, please ignore this email.\n\n" +
                            "TALOMS System\n" +
                            "Traditional Authority Land & Occupancy Management System"
            );

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}",
                    toEmail, e.getMessage());
        }
    }
}