package za.co.taloms.security.application.service;

import org.junit.jupiter.api.Disabled;
import za.co.taloms.BaseTest;
import za.co.taloms.security.SecurityTestUtils;
import za.co.taloms.security.application.dto.UserCreateRequest;
import za.co.taloms.security.application.dto.UserUpdateRequest;
import za.co.taloms.security.domain.repository.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("Fix role seeding in test database - requires H2 compatibility")
@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest extends BaseTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepositoryPort userRepository;

    @BeforeEach
    void setUp() {
        SecurityTestUtils.authenticateAsAdmin();
    }

    @Test
    void shouldCreateUser() {
        var request = UserCreateRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .password("Password123!")
                .roleName("ROLE_TA_ADMINISTRATOR")
                .build();

        var response = userService.createUser(request);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        assertTrue(response.getEnabled());
    }

    @Test
    void shouldFindUserById() {
        var request = UserCreateRequest.builder()
                .username("finduser")
                .email("find@example.com")
                .fullName("Find User")
                .password("Password123!")
                .roleName("ROLE_TA_ADMINISTRATOR")
                .build();

        var created = userService.createUser(request);
        var found = userService.findById(created.getId());

        assertNotNull(found);
        assertEquals(created.getId(), found.getId());
        assertEquals("finduser", found.getUsername());
    }

    @Test
    void shouldUpdateUser() {
        var request = UserCreateRequest.builder()
                .username("updateuser")
                .email("update@example.com")
                .fullName("Update User")
                .password("Password123!")
                .roleName("ROLE_TA_ADMINISTRATOR")
                .build();

        var created = userService.createUser(request);

        var updateRequest = UserUpdateRequest.builder()
                .fullName("Updated User")
                .email("updated@example.com")
                .roleName("ROLE_SYSTEM_ADMIN")
                .build();

        var updated = userService.updateUser(created.getId(), updateRequest);

        assertEquals("Updated User", updated.getFullName());
        assertEquals("updated@example.com", updated.getEmail());
    }

    @Test
    void shouldLockUser() {
        var request = UserCreateRequest.builder()
                .username("lockuser")
                .email("lock@example.com")
                .fullName("Lock User")
                .password("Password123!")
                .roleName("ROLE_TA_ADMINISTRATOR")
                .build();

        var created = userService.createUser(request);
        userService.lockUser(created.getId());

        var locked = userService.findById(created.getId());
        assertTrue(locked.getAccountLocked());
    }

    @Test
    void shouldUnlockUser() {
        var request = UserCreateRequest.builder()
                .username("unlockuser")
                .email("unlock@example.com")
                .fullName("Unlock User")
                .password("Password123!")
                .roleName("ROLE_TA_ADMINISTRATOR")
                .build();

        var created = userService.createUser(request);
        userService.lockUser(created.getId());
        userService.unlockUser(created.getId());

        var unlocked = userService.findById(created.getId());
        assertFalse(unlocked.getAccountLocked());
    }

    @Test
    void shouldActivateUser() {
        var request = UserCreateRequest.builder()
                .username("activateuser")
                .email("activate@example.com")
                .fullName("Activate User")
                .password("Password123!")
                .roleName("ROLE_TA_ADMINISTRATOR")
                .build();

        var created = userService.createUser(request);
        userService.activateUser(created.getId());

        var activated = userService.findById(created.getId());
        assertTrue(activated.getEnabled());
    }

    @Test
    void shouldCountAllUsers() {
        var count = userService.countAll();
        assertTrue(count >= 0);
    }

    @Test
    void shouldCountActiveUsers() {
        var activeCount = userService.countActive();
        assertTrue(activeCount >= 0);
    }
}