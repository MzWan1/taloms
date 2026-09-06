package za.co.taloms.security.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.taloms.security.application.dto.LoginRequest;
import za.co.taloms.security.domain.entity.Role;
import za.co.taloms.security.domain.entity.User;
import za.co.taloms.security.domain.repository.UserRepositoryPort;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Test
    void successfulLoginShouldUpdateLastLoginAtAndPersistUser() {
        var user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hash")
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(3)
                .roles(Set.of(Role.builder().name("ROLE_ADMIN").build()))
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(mock(UserDetails.class));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");
        when(jwtService.getExpirationTime()).thenReturn(3600L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authenticationService.login(
                LoginRequest.builder().username("testuser").password("Password123!").build());

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("token", response.getAccessToken());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(captor.capture());
        var savedUser = captor.getValue();
        assertEquals(0, savedUser.getFailedLoginAttempts());
        assertNotNull(savedUser.getLastLoginAt());
        assertTrue(savedUser.getLastLoginAt().isAfter(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    void lockedAccountShouldRejectLoginBeforeAuthentication() {
        var user = User.builder()
                .username("lockeduser")
                .accountLocked(true)
                .build();

        when(userRepository.findByUsername("lockeduser")).thenReturn(Optional.of(user));

        assertThrows(LockedException.class, () -> authenticationService.login(
                LoginRequest.builder().username("lockeduser").password("Password123!").build()));

        verifyNoInteractions(authenticationManager, jwtService, userDetailsService);
        verify(userRepository, never()).save(any());
    }

    @Test
    void invalidCredentialsShouldIncrementFailedAttempts() {
        var user = User.builder()
                .username("testuser")
                .accountLocked(false)
                .failedLoginAttempts(1)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("invalid"));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(BadCredentialsException.class, () -> authenticationService.login(
                LoginRequest.builder().username("testuser").password("wrong").build()));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getFailedLoginAttempts());
        assertNull(captor.getValue().getLastLoginAt());
        verifyNoInteractions(jwtService, userDetailsService);
    }
}