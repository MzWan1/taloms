package za.co.taloms.security.domain.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldCreateUser() {
        var user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .enabled(true)
                .build();

        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getFullName());
        assertTrue(user.getEnabled());
    }

    @Test
    void shouldLockUser() {
        var user = User.builder()
                .username("testuser")
                .accountLocked(false)
                .build();

        user.setAccountLocked(true);
        assertTrue(user.getAccountLocked());
    }

    @Test
    void shouldEnableUser() {
        var user = User.builder()
                .username("testuser")
                .enabled(false)
                .build();

        user.setEnabled(true);
        assertTrue(user.getEnabled());
    }

    @Test
    void shouldIncrementFailedLoginAttempts() {
        var user = User.builder()
                .username("testuser")
                .failedLoginAttempts(0)
                .build();

        user.setFailedLoginAttempts(1);
        assertEquals(1, user.getFailedLoginAttempts());
    }
}