package za.co.taloms.useraccess.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.entity.PTOPurpose;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;
import za.co.taloms.security.domain.entity.Role;
import za.co.taloms.security.domain.entity.User;
import za.co.taloms.security.domain.repository.UserRepositoryPort;
import za.co.taloms.useraccess.application.dto.PTOAccessResponse;
import za.co.taloms.useraccess.application.dto.UserAccessDto;
import za.co.taloms.useraccess.domain.entity.UserPTOAccess;
import za.co.taloms.useraccess.domain.repository.UserPTOAccessRepositoryPort;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccessServiceImplTest {

    @Mock private UserRepositoryPort          userRepository;
    @Mock private PTORepositoryPort            ptoRepository;
    @Mock private UserPTOAccessRepositoryPort accessRepository;

    private UserAccessServiceImpl service;

    private static final String OWNER_ID  = "9001015800085";
    private static final String OTHER_ID  = "8507251234567";

    @BeforeEach
    void setUp() {
        service = new UserAccessServiceImpl(userRepository, ptoRepository, accessRepository);
    }

    private User user(Long id, String username, String idNumber, String... roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String r : roleNames) {
            roles.add(Role.builder().name(r).build());
        }
        return User.builder()
                .id(id)
                .username(username)
                .email(username + "@test.com")
                .passwordHash("hash")
                .fullName("Full " + username)
                .idNumber(idNumber)
                .enabled(true)
                .roles(roles)
                .build();
    }

    private PTO pto(Long id, String idNumber, boolean deleted) {
        PTO p = PTO.builder()
                .id(id)
                .ptoNumber("PTO-" + id)
                .ptoHolderName("Holder " + id)
                .idNumber(idNumber)
                .purpose(PTOPurpose.RESIDENTIAL)
                .status(PTOStatus.ACTIVE)
                .issueDate(LocalDate.now())
                .build();
                if (deleted) p.setDeletedAt(java.time.LocalDateTime.now());
        return p;
    }

    // ── findOwnedPtoIds ───────────────────────────────────────────────────────

        @Test
    void shouldFindOwnedPtosByIdNumberMatch() {
        User owner = user(1L, "owner", OWNER_ID, "ROLE_USER");
        PTO pto1 = pto(10L, OWNER_ID, false);
        PTO pto2 = pto(11L, OWNER_ID, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(ptoRepository.findByIdNumber(OWNER_ID)).thenReturn(List.of(pto1, pto2));

        List<Long> owned = service.findOwnedPtoIds(1L);

        assertEquals(List.of(10L, 11L), owned);
    }

    @Test
    void shouldReturnEmptyWhenUserHasNoIdNumber() {
        User owner = user(1L, "owner", null, "ROLE_USER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertTrue(service.findOwnedPtoIds(1L).isEmpty());
        verifyNoInteractions(ptoRepository);
    }

    @Test
    void shouldExcludeDeletedPtosFromOwned() {
        User owner = user(1L, "owner", OWNER_ID, "ROLE_USER");
        PTO deleted = pto(10L, OWNER_ID, true);
        PTO active  = pto(11L, OWNER_ID, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(ptoRepository.findByIdNumber(OWNER_ID)).thenReturn(List.of(deleted, active));

        List<Long> owned = service.findOwnedPtoIds(1L);

        assertEquals(List.of(11L), owned);
    }

    @Test
    void shouldThrowWhenUserNotFoundForOwned() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.findOwnedPtoIds(99L));
    }

    // ── findAccessiblePtoIds ──────────────────────────────────────────────────

    @Test
    void shouldIncludeOwnedAndLinkedPtos() {
        User owner = user(1L, "owner", OWNER_ID, "ROLE_USER");
        PTO owned = pto(10L, OWNER_ID, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(ptoRepository.findByIdNumber(OWNER_ID)).thenReturn(List.of(owned));
        when(accessRepository.findAllByUserId(1L)).thenReturn(List.of());

        List<Long> accessible = service.findAccessiblePtoIds(1L);
        assertEquals(List.of(10L), accessible);

        // Linked user sees PTO 20 via user_pto_access link
        User linkedUser = user(2L, "linked", OTHER_ID, "ROLE_USER");
        PTO linked = pto(20L, OWNER_ID, false);

        when(userRepository.findById(2L)).thenReturn(Optional.of(linkedUser));
        when(ptoRepository.findByIdNumber(OTHER_ID)).thenReturn(List.of());
        when(accessRepository.findAllByUserId(2L)).thenReturn(
                List.of(UserPTOAccess.builder().userId(2L).ptoId(20L).build()));
        when(ptoRepository.findById(20L)).thenReturn(Optional.of(linked));

        List<Long> accessibleLinked = service.findAccessiblePtoIds(2L);
        assertTrue(accessibleLinked.contains(20L));
    }

    @Test
    void shouldNotIncludeDeletedLinkedPto() {
        User user = user(2L, "linked", OTHER_ID, "ROLE_USER");
        PTO linkedActive = pto(20L, OWNER_ID, false);
        PTO linkedDeleted = pto(21L, OWNER_ID, true);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(ptoRepository.findByIdNumber(OTHER_ID)).thenReturn(List.of());
        when(accessRepository.findAllByUserId(2L)).thenReturn(List.of(
                UserPTOAccess.builder().userId(2L).ptoId(20L).build(),
                UserPTOAccess.builder().userId(2L).ptoId(21L).build()));
        when(ptoRepository.findById(20L)).thenReturn(Optional.of(linkedActive));
        when(ptoRepository.findById(21L)).thenReturn(Optional.of(linkedDeleted));

        List<Long> accessible = service.findAccessiblePtoIds(2L);
        assertTrue(accessible.contains(20L));
        assertFalse(accessible.contains(21L));
    }

    // ── ownsPto ───────────────────────────────────────────────────────────────

    @Test
    void shouldReturnTrueWhenIdNumbersMatch() {
        User owner = user(1L, "owner", OWNER_ID, "ROLE_USER");
        PTO pto = pto(10L, OWNER_ID, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(ptoRepository.findById(10L)).thenReturn(Optional.of(pto));

        assertTrue(service.ownsPto(1L, 10L));
    }

    @Test
    void shouldReturnFalseWhenIdNumbersDoNotMatch() {
        User user = user(1L, "user", OTHER_ID, "ROLE_USER");
        PTO pto = pto(10L, OWNER_ID, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(ptoRepository.findById(10L)).thenReturn(Optional.of(pto));

        assertFalse(service.ownsPto(1L, 10L));
    }

    @Test
    void shouldReturnFalseWhenPtoIsDeleted() {
        User owner = user(1L, "owner", OWNER_ID, "ROLE_USER");
        PTO pto = pto(10L, OWNER_ID, true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(ptoRepository.findById(10L)).thenReturn(Optional.of(pto));

        assertFalse(service.ownsPto(1L, 10L));
    }

    @Test
    void shouldReturnFalseWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
                assertFalse(service.ownsPto(99L, 10L));
    }

    // ── hasAccess ─────────────────────────────────────────────────────────────

    @Test
    void shouldReturnTrueWhenUserOwnsPto() {
        User owner = user(1L, "owner", OWNER_ID, "ROLE_USER");
        PTO pto = pto(10L, OWNER_ID, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(ptoRepository.findById(10L)).thenReturn(Optional.of(pto));

        assertTrue(service.hasAccess(1L, 10L));
    }

    @Test
    void shouldReturnTrueWhenUserIsLinked() {
        User user = user(2L, "linked", OTHER_ID, "ROLE_USER");
        PTO pto = pto(20L, OWNER_ID, false);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(ptoRepository.findById(20L)).thenReturn(Optional.of(pto));
        when(accessRepository.existsByUserIdAndPtoId(2L, 20L)).thenReturn(true);

        assertTrue(service.hasAccess(2L, 20L));
    }

    @Test
    void shouldReturnFalseWhenNotOwnerAndNotLinked() {
        User user = user(2L, "linked", OTHER_ID, "ROLE_USER");
        PTO pto = pto(20L, OWNER_ID, false);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(ptoRepository.findById(20L)).thenReturn(Optional.of(pto));
        when(accessRepository.existsByUserIdAndPtoId(2L, 20L)).thenReturn(false);

                assertFalse(service.hasAccess(2L, 20L));
    }

    // ── listLinkedUsers ───────────────────────────────────────────────────────

    @Test
    void shouldListLinkedUsersForOwner() {
        User owner = user(1L, "owner", OWNER_ID, "ROLE_USER");
        User linkedUser = user(2L, "linked", OTHER_ID, "ROLE_USER");
        PTO pto = pto(10L, OWNER_ID, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(ptoRepository.findById(10L)).thenReturn(Optional.of(pto));
        when(accessRepository.findAllByPtoId(10L)).thenReturn(List.of(
                UserPTOAccess.builder().userId(2L).ptoId(10L).build()));
        when(userRepository.findById(2L)).thenReturn(Optional.of(linkedUser));

        List<PTOAccessResponse> result = service.listLinkedUsers(10L, 1L);

        // owner + one linked = 2
        assertEquals(2, result.size());

        PTOAccessResponse ownerResp = result.stream()
                .filter(PTOAccessResponse::isOwner)
                .findFirst().orElseThrow();
        assertEquals(1L, ownerResp.getUserId());

        PTOAccessResponse linkedResp = result.stream()
                .filter(r -> !r.isOwner())
                .findFirst().orElseThrow();
        assertEquals(2L, linkedResp.getUserId());
        assertEquals("linked@test.com", linkedResp.getEmail());
    }

    @Test
    void shouldRejectListLinkedUsersForNonOwner() {
        User user = user(2L, "user", OTHER_ID, "ROLE_USER");
        PTO pto = pto(10L, OWNER_ID, false);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(ptoRepository.findById(10L)).thenReturn(Optional.of(pto));

        assertThrows(SecurityException.class,
                                () -> service.listLinkedUsers(10L, 2L));
    }

    // ── addLinkedUser ─────────────────────────────────────────────────────────

    @Test
    void shouldAddLinkedUserForOwner() {
        User owner = user(1L, "owner", OWNER_ID, "ROLE_USER");
        User target = user(2L, "target", OTHER_ID, "ROLE_USER");
        PTO pto = pto(10L, OWNER_ID, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(ptoRepository.findById(10L)).thenReturn(Optional.of(pto));
        when(accessRepository.existsByUserIdAndPtoId(2L, 10L)).thenReturn(false);

        service.addLinkedUser(10L, 2L, 1L, "owner");

        ArgumentCaptor<UserPTOAccess> captor = ArgumentCaptor.forClass(UserPTOAccess.class);
        verify(accessRepository).save(captor.capture());
        assertEquals(2L, captor.getValue().getUserId());
        assertEquals(10L, captor.getValue().getPtoId());
        assertEquals("owner", captor.getValue().getCreatedBy());
    }

    @Test
    void shouldRejectAddLinkedUserForNonOwner() {
        User nonOwner = user(2L, "user", OTHER_ID, "ROLE_USER");
        PTO pto = pto(10L, OWNER_ID, false);

        when(userRepository.findById(2L)).thenReturn(Optional.of(nonOwner));
        when(ptoRepository.findById(10L)).thenReturn(Optional.of(pto));

        assertThrows(SecurityException.class,
                                () -> service.addLinkedUser(10L, 3L, 2L, "user"));
    }

    @Test
    void shouldRejectAddWhenTargetAlreadyOwnsPto() {
        User owner = user(1L, "owner", OWNER_ID, "ROLE_USER");
        User target = user(2L, "target", OWNER_ID, "ROLE_USER");
        PTO pto = pto(10L, OWNER_ID, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(ptoRepository.findById(10L)).thenReturn(Optional.of(pto));

        assertThrows(BusinessValidationException.class,
                () -> service.addLinkedUser(10L, 2L, 1L, "owner"));
    }

    @Test
    void shouldRejectAddWhenAlreadyLinked() {
        User owner = user(1L, "owner", OWNER_ID, "ROLE_USER");
        User target = user(2L, "target", OTHER_ID, "ROLE_USER");
        PTO pto = pto(10L, OWNER_ID, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(ptoRepository.findById(10L)).thenReturn(Optional.of(pto));
        when(accessRepository.existsByUserIdAndPtoId(2L, 10L)).thenReturn(true);

        assertThrows(BusinessValidationException.class,
                () -> service.addLinkedUser(10L, 2L, 1L, "owner"));
    }

    @Test
    void shouldThrowWhenTargetUserNotFoundForAdd() {
        User owner = user(1L, "owner", OWNER_ID, "ROLE_USER");
        PTO pto = pto(10L, OWNER_ID, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        when(ptoRepository.findById(10L)).thenReturn(Optional.of(pto));

                assertThrows(ResourceNotFoundException.class,
                () -> service.addLinkedUser(10L, 2L, 1L, "owner"));
    }

    // ── removeLinkedUser ──────────────────────────────────────────────────────

    @Test
    void shouldRemoveLinkedUserForOwner() {
        User owner = user(1L, "owner", OWNER_ID, "ROLE_USER");
        User target = user(2L, "target", OTHER_ID, "ROLE_USER");
        PTO pto = pto(10L, OWNER_ID, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(ptoRepository.findById(10L)).thenReturn(Optional.of(pto));
        when(accessRepository.existsByUserIdAndPtoId(2L, 10L)).thenReturn(true);

        service.removeLinkedUser(10L, 2L, 1L);

        verify(accessRepository).deleteByUserIdAndPtoId(2L, 10L);
    }

    @Test
    void shouldRejectRemoveForNonOwner() {
        User nonOwner = user(2L, "user", OTHER_ID, "ROLE_USER");
        PTO pto = pto(10L, OWNER_ID, false);

        when(userRepository.findById(2L)).thenReturn(Optional.of(nonOwner));
        when(ptoRepository.findById(10L)).thenReturn(Optional.of(pto));

        assertThrows(SecurityException.class,
                                () -> service.removeLinkedUser(10L, 3L, 2L));
    }

    @Test
    void shouldRejectRemoveOwnerFromOwnPto() {
        User owner = user(1L, "owner", OWNER_ID, "ROLE_USER");
        PTO pto = pto(10L, OWNER_ID, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(ptoRepository.findById(10L)).thenReturn(Optional.of(pto));

        assertThrows(BusinessValidationException.class,
                () -> service.removeLinkedUser(10L, 1L, 1L));
    }

    @Test
    void shouldRejectRemoveWhenNotLinked() {
        User owner = user(1L, "owner", OWNER_ID, "ROLE_USER");
        User target = user(2L, "target", OTHER_ID, "ROLE_USER");
        PTO pto = pto(10L, OWNER_ID, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(ptoRepository.findById(10L)).thenReturn(Optional.of(pto));
        when(accessRepository.existsByUserIdAndPtoId(2L, 10L)).thenReturn(false);

        assertThrows(BusinessValidationException.class,
                                () -> service.removeLinkedUser(10L, 2L, 1L));
    }

    // ── searchUsers ───────────────────────────────────────────────────────────

    @Test
    void shouldSearchOnlyRoleUserAccounts() {
        User user1 = user(1L, "resident1", "9001015800085", "ROLE_USER");
        User user2 = user(2L, "admin1", "8507251234567", "ROLE_ADMIN");
        User user3 = user(3L, "chief1", "7001011234567", "ROLE_CHIEF");
        User user4 = user(4L, "disabled1", "6507251234567", "ROLE_USER");
        user4.setEnabled(false);

        when(userRepository.searchByNameOrEmail("resident"))
                .thenReturn(List.of(user1, user4));

        List<UserAccessDto> results = service.searchUsers("resident");

        assertEquals(1, results.size());
        assertEquals("resident1@test.com", results.get(0).getEmail());
        assertEquals("9001015800085", results.get(0).getIdNumber());
    }

    @Test
    void shouldReturnEmptyForBlankQuery() {
        assertTrue(service.searchUsers("").isEmpty());
        assertTrue(service.searchUsers("   ").isEmpty());
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldExcludeSelfAndAlreadyLinkedFromLinkableSearch() {
        User me = user(1L, "owner", OWNER_ID, "ROLE_USER");
        User linked = user(2L, "linked", "8001015800081", "ROLE_USER");
        User candidate = user(3L, "candidate", "8101015800082", "ROLE_USER");

        when(userRepository.searchByNameOrEmail("a"))
                .thenReturn(List.of(me, linked, candidate));
        when(ptoRepository.findById(10L)).thenReturn(Optional.of(pto(10L, OWNER_ID, false)));
        when(accessRepository.existsByUserIdAndPtoId(2L, 10L)).thenReturn(true);
        // (3L, 10L) intentionally unstubbed: the mock defaults to false (not linked).

        List<UserAccessDto> results = service.searchLinkableUsers("a", 10L, 1L);

        assertEquals(1, results.size());
        assertEquals(3L, results.get(0).getId());
    }
}








