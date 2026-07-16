package za.co.taloms.security.application.service;

import za.co.taloms.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JwtServiceTest extends BaseTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldGenerateToken() {
        UserDetails userDetails = new User(
                "testuser",
                "password",
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
        );

        var token = jwtService.generateToken(userDetails);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldExtractUsername() {
        UserDetails userDetails = new User(
                "extractuser",
                "password",
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
        );

        var token = jwtService.generateToken(userDetails);
        var username = jwtService.extractUsername(token);

        assertEquals("extractuser", username);
    }

    @Test
    void shouldValidateToken() {
        UserDetails userDetails = new User(
                "validuser",
                "password",
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
        );

        var token = jwtService.generateToken(userDetails);
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }
}