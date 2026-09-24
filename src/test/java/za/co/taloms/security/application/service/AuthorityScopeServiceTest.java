package za.co.taloms.security.application.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import za.co.taloms.security.domain.entity.Role;
import za.co.taloms.security.domain.entity.User;
import za.co.taloms.security.domain.repository.UserRepositoryPort;
import za.co.taloms.traditionalauthority.domain.repository.TraditionalAuthorityRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorityScopeServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private TraditionalAuthorityRepositoryPort authorityRepository;

    @InjectMocks
    private AuthorityScopeService scopeService;

    private Role adminRole;
    private Role chiefRole;
    private Role headsmanRole;

    @BeforeEach
    void setUp() {
        adminRole    = Role.builder().id(1L).name("ROLE_ADMIN").build();
        chiefRole    = Role.builder().id(2L).name("ROLE_CHIEF").build();
        headsmanRole = Role.builder().id(3L).name("ROLE_HEADSMAN").build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(User user) {
        var auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByUsername(user.getUsername()))
                .thenReturn(Optional.of(user));
    }

    private User userWith(Long id, String username, Role... roles) {
        return User.builder()
                .id(id)
                .username(username)
                .roles(new java.util.HashSet<>(Set.of(roles)))
                .build();
    }

    /** ADMIN: unrestricted access. */
    @Test
    void adminCanAccessAnyAuthority() {
        var admin = userWith(1L, "admin", adminRole);
        authenticate(admin);
        assertTrue(scopeService.isCurrentUserAdmin());
        assertFalse(scopeService.isCurrentUserChiefOrHeadsman());
        assertTrue(scopeService.canAccessAuthority(99L));
        assertNull(scopeService.getCurrentUserAuthorityId());
    }

    /** Chief with a single authority via the join table. */
    @Test
    void chiefWithJoinTableLinkIsScopedToTheirAuthority() {
        var chief = userWith(10L, "chief", chiefRole);
        authenticate(chief);
        when(userRepository.findAuthorityIdsByUserId(10L))
                .thenReturn(List.of(5L));
        assertEquals(Set.of(5L), scopeService.getCurrentUserAuthorityIds());
        assertEquals(5L, scopeService.getCurrentUserAuthorityId());
        assertTrue(scopeService.canAccessAuthority(5L));
        assertFalse(scopeService.canAccessAuthority(6L));
    }

    /** Chief with multiple authorities via the join table. */
    @Test
    void chiefWithMultipleAuthoritiesViaJoinTable() {
        var chief = userWith(10L, "chief", chiefRole);
        authenticate(chief);
        when(userRepository.findAuthorityIdsByUserId(10L))
                .thenReturn(List.of(5L, 6L));
        Set<Long> ids = scopeService.getCurrentUserAuthorityIds();
        assertEquals(Set.of(5L, 6L), ids);
        assertTrue(scopeService.canAccessAuthority(5L));
        assertTrue(scopeService.canAccessAuthority(6L));
        assertFalse(scopeService.canAccessAuthority(7L));
        assertTrue(Set.of(5L, 6L).contains(scopeService.getCurrentUserAuthorityId()));
    }

    /** Legacy authority-side link (chief_id on authority) still resolves scope. */
    @Test
    void chiefWithAuthoritySideLinkIsScopedToTheirAuthority() {
        var chief = userWith(10L, "chief", chiefRole);
        authenticate(chief);
        when(authorityRepository.findIdsByChiefIdOrHeadmanId(10L))
                .thenReturn(List.of(5L));
        assertEquals(Set.of(5L), scopeService.getCurrentUserAuthorityIds());
        assertEquals(5L, scopeService.getCurrentUserAuthorityId());
        assertTrue(scopeService.canAccessAuthority(5L));
        assertFalse(scopeService.canAccessAuthority(6L));
    }

    /** Headsman with authority-side link. */
    @Test
    void headmanWithAuthoritySideLinkIsScopedToTheirAuthority() {
        var headsman = userWith(30L, "headsman", headsmanRole);
        authenticate(headsman);
        when(authorityRepository.findIdsByChiefIdOrHeadmanId(30L))
                .thenReturn(List.of(7L));
        assertEquals(Set.of(7L), scopeService.getCurrentUserAuthorityIds());
        assertEquals(7L, scopeService.getCurrentUserAuthorityId());
        assertTrue(scopeService.canAccessAuthority(7L));
        assertFalse(scopeService.canAccessAuthority(8L));
    }

    /** Chief with no authorities at all. */
    @Test
    void unlinkedChiefSeesNothing() {
        var chief = userWith(10L, "chief", chiefRole);
        authenticate(chief);
        when(userRepository.findAuthorityIdsByUserId(10L))
                .thenReturn(List.of());
        assertTrue(scopeService.isCurrentUserChiefOrHeadsman());
        assertTrue(scopeService.getCurrentUserAuthorityIds().isEmpty());
        assertNull(scopeService.getCurrentUserAuthorityId());
        assertFalse(scopeService.canAccessAuthority(5L));
        assertThrows(SecurityException.class,
                () -> scopeService.requireAuthorityAccess(5L));
    }

    @Test
    void requireAuthorityAccessThrowsForForeignAuthority() {
        var chief = userWith(10L, "chief", chiefRole);
        authenticate(chief);
        when(userRepository.findAuthorityIdsByUserId(10L))
                .thenReturn(List.of(5L));
        assertThrows(SecurityException.class,
                () -> scopeService.requireAuthorityAccess(6L));
        assertDoesNotThrow(() -> scopeService.requireAuthorityAccess(5L));
    }

    @Test
    void requireAdminThrowsForNonAdmin() {
        var chief = userWith(10L, "chief", chiefRole);
        authenticate(chief);
        assertThrows(SecurityException.class, () -> scopeService.requireAdmin());
    }
}


