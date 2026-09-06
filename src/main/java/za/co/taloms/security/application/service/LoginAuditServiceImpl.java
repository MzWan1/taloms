package za.co.taloms.security.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.security.domain.repository.UserRepositoryPort;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAuditServiceImpl implements LoginAuditService {

    private final UserRepositoryPort userRepository;

    @Override
    @Transactional
    public void recordSuccessfulLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }
}