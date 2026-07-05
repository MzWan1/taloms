package za.co.taloms.security.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import za.co.taloms.common.ApplicationConstants;
import za.co.taloms.security.application.dto.LoginRequest;
import za.co.taloms.security.application.dto.LoginResponse;
import za.co.taloms.security.domain.repository.UserRepositoryPort;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService            jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserRepositoryPort    userRepository;

    @Override
    public LoginResponse login(LoginRequest request) {

        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid username or password"));

        if (user.getAccountLocked()) {
            throw new LockedException("Account is locked. "
                    + "Please contact your administrator.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));

        } catch (AuthenticationException e) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= ApplicationConstants.MAX_FAILED_LOGIN_ATTEMPTS) {
                user.setAccountLocked(true);
                log.warn("Account locked for user: {} after {} failed attempts",
                        user.getUsername(), attempts);
            }

            userRepository.save(user);
            throw new BadCredentialsException("Invalid username or password");
        }

        // Successful login — reset failed attempts
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        var userDetails = userDetailsService
                .loadUserByUsername(user.getUsername());

        var token = jwtService.generateToken(userDetails);

        var roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());

        log.info("User logged in successfully: {}", user.getUsername());

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roles(roles)
                .build();
    }
}