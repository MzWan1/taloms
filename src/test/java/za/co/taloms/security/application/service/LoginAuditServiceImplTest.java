package za.co.taloms.security.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.taloms.security.domain.entity.User;
import za.co.taloms.security.domain.repository.UserRepositoryPort;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAuditServiceImplTest {

    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private LoginAuditServiceImpl loginAuditService;

    @Test
    void recordSuccessfulLoginShouldPersistTimestamp() {
        var user = User.builder().username("testuser").build();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        loginAuditService.recordSuccessfulLogin("testuser");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertNotNull(captor.getValue().getLastLoginAt());
        assertTrue(captor.getValue().getLastLoginAt().isAfter(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    void recordSuccessfulLoginShouldIgnoreUnknownUsers() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        loginAuditService.recordSuccessfulLogin("ghost");

        verify(userRepository, never()).save(any());
    }
}