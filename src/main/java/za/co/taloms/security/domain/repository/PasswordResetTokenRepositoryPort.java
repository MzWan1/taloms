package za.co.taloms.security.domain.repository;

import za.co.taloms.security.domain.entity.PasswordResetToken;
import java.util.Optional;

public interface PasswordResetTokenRepositoryPort {
    PasswordResetToken save(PasswordResetToken token);
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUserId(Long userId);
}