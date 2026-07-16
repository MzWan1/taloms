package za.co.taloms.security.domain.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void shouldCreateRole() {
        var role = Role.builder()
                .name("ROLE_SYSTEM_ADMIN")
                .description("System Administrator")
                .build();

        assertEquals("ROLE_SYSTEM_ADMIN", role.getName());
        assertEquals("System Administrator", role.getDescription());
    }
}