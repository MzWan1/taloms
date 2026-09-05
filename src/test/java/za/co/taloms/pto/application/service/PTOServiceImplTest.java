package za.co.taloms.pto.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.document.application.service.DocumentService;
import za.co.taloms.parcel.domain.repository.ParcelRepositoryPort;
import za.co.taloms.pto.application.dto.PTOApprovalRequest;
import za.co.taloms.pto.application.dto.PTOResponse;
import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.entity.PTOPurpose;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.pto.domain.repository.PTOApprovalSignatureRepositoryPort;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;
import za.co.taloms.security.domain.entity.Role;
import za.co.taloms.security.domain.entity.User;
import za.co.taloms.security.domain.repository.UserRepositoryPort;
import za.co.taloms.traditionalauthority.domain.repository.TraditionalAuthorityRepositoryPort;
import za.co.taloms.traditionalauthority.domain.repository.VillageRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Tests the PTO approval ownership rule:
 * only a CHIEF may approve PTOs — never an admin or headsman.
 */
@ExtendWith(MockitoExtension.class)
class PTOServiceImplTest {

    @Mock private PTORepositoryPort ptoRepository;
    @Mock private ParcelRepositoryPort parcelRepository;
    @Mock private PTONumberGenerator numberGenerator;
    @Mock private TraditionalAuthorityRepositoryPort authorityRepository;
    @Mock private VillageRepositoryPort villageRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private DocumentService documentService;
    @Mock private PTOApprovalSignatureRepositoryPort signatureRepository;
    @Mock private UserRepositoryPort userRepository;

    private PTOServiceImpl service;

    @BeforeEach
    void setUp() {
        // Order must match the field declaration order of PTOServiceImpl
        service = new PTOServiceImpl(
                ptoRepository, parcelRepository, numberGenerator,
                authorityRepository, villageRepository, eventPublisher,
                documentService, signatureRepository, userRepository);
    }

    private PTO pendingPTO(String createdBy) {
        return PTO.builder()
                .id(1L)
                .ptoNumber("PTO-2026-00001")
                .ptoHolderName("Test Holder")
                .idNumber("9001015800085")
                .purpose(PTOPurpose.RESIDENTIAL)
                .status(PTOStatus.PENDING)
                .createdBy(createdBy)
                .build();
    }

    private User user(String username, String roleName) {
        var role = Role.builder().name(roleName).build();
        return User.builder()
                .username(username)
                .email(username + "@test.com")
                .passwordHash("secret")
                .roles(Set.of(role))
                .build();
    }

    private void stubSuccessfulApproval(PTO pto) {
        when(ptoRepository.findById(1L)).thenReturn(Optional.of(pto));
        when(documentService.getMissingRequiredDocumentTypes(any(), anyLong())).thenReturn(List.of());
        when(ptoRepository.save(any(PTO.class))).thenAnswer(inv -> inv.getArgument(0));
    }
    @Test
    void shouldAllowChiefToApprovePTOCreatedByAdmin() {
        var pto = pendingPTO("adminuser");
        stubSuccessfulApproval(pto);
        when(userRepository.findByUsername("adminuser")).thenReturn(Optional.of(user("adminuser", "ROLE_ADMIN")));
        when(userRepository.findByUsername("chiefuser")).thenReturn(Optional.of(user("chiefuser", "ROLE_CHIEF")));

        PTOResponse response = service.approvePTO(1L, new PTOApprovalRequest(), "chiefuser");

        assertNotNull(response);
        assertEquals(PTOStatus.ACTIVE, response.getStatus());
        assertEquals("chiefuser", response.getApprovedBy());
    }

    @Test
    void shouldRejectAdminApprovingPTOCreatedByAdmin() {
        var pto = pendingPTO("adminuser");
        when(ptoRepository.findById(1L)).thenReturn(Optional.of(pto));
        when(userRepository.findByUsername("adminuser")).thenReturn(Optional.of(user("adminuser", "ROLE_ADMIN")));
        when(userRepository.findByUsername("admin2")).thenReturn(Optional.of(user("admin2", "ROLE_ADMIN")));

        var ex = assertThrows(BusinessValidationException.class,
                () -> service.approvePTO(1L, new PTOApprovalRequest(), "admin2"));

        assertTrue(ex.getMessage().contains("Only a chief may approve it."));
    }

    @Test
    void shouldRejectHeadsmanApprovingPTO() {
        var pto = pendingPTO("adminuser");
        when(ptoRepository.findById(1L)).thenReturn(Optional.of(pto));
        when(userRepository.findByUsername("adminuser")).thenReturn(Optional.of(user("adminuser", "ROLE_ADMIN")));
        when(userRepository.findByUsername("headsman")).thenReturn(Optional.of(user("headsman", "ROLE_HEADSMAN")));

        var ex = assertThrows(BusinessValidationException.class,
                () -> service.approvePTO(1L, new PTOApprovalRequest(), "headsman"));

        assertTrue(ex.getMessage().contains("Only a chief may approve it."));
    }

    @Test
    void shouldRejectUnknownApprover() {
        var pto = pendingPTO("adminuser");
        when(ptoRepository.findById(1L)).thenReturn(Optional.of(pto));
        when(userRepository.findByUsername("adminuser")).thenReturn(Optional.of(user("adminuser", "ROLE_ADMIN")));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        var ex = assertThrows(BusinessValidationException.class,
                () -> service.approvePTO(1L, new PTOApprovalRequest(), "ghost"));

        assertTrue(ex.getMessage().contains("Only a chief may approve it."));
    }

    @Test
    void shouldAllowChiefToApproveOwnPTO() {
        var pto = pendingPTO("chiefuser");
        stubSuccessfulApproval(pto);
        when(userRepository.findByUsername("chiefuser")).thenReturn(Optional.of(user("chiefuser", "ROLE_CHIEF")));

        PTOResponse response = service.approvePTO(1L, new PTOApprovalRequest(), "chiefuser");

        assertNotNull(response);
        assertEquals(PTOStatus.ACTIVE, response.getStatus());
        assertEquals("chiefuser", response.getApprovedBy());
    }

    @Test
    void shouldRejectOtherChiefApprovingChiefCreatedPTO() {
        var pto = pendingPTO("chiefA");
        when(ptoRepository.findById(1L)).thenReturn(Optional.of(pto));
        when(userRepository.findByUsername("chiefA")).thenReturn(Optional.of(user("chiefA", "ROLE_CHIEF")));
        // The "only that chief" username check fails before the approver is looked up,
        // so chiefB's user record is never loaded — no stub for it here.

        var ex = assertThrows(BusinessValidationException.class,
                () -> service.approvePTO(1L, new PTOApprovalRequest(), "chiefB"));

        assertTrue(ex.getMessage().contains("Only that chief may approve it."));
    }

    @Test
    void shouldAllowChiefToApproveLegacyPTOWithoutCreator() {
        var pto = pendingPTO(null);
        stubSuccessfulApproval(pto);
        when(userRepository.findByUsername("chiefuser")).thenReturn(Optional.of(user("chiefuser", "ROLE_CHIEF")));

        PTOResponse response = service.approvePTO(1L, new PTOApprovalRequest(), "chiefuser");

        assertNotNull(response);
        assertEquals(PTOStatus.ACTIVE, response.getStatus());
        assertEquals("chiefuser", response.getApprovedBy());
    }

    @Test
    void shouldRejectAdminApprovingLegacyPTOWithoutCreator() {
        var pto = pendingPTO(null);
        when(ptoRepository.findById(1L)).thenReturn(Optional.of(pto));
        when(userRepository.findByUsername("adminuser")).thenReturn(Optional.of(user("adminuser", "ROLE_ADMIN")));

        var ex = assertThrows(BusinessValidationException.class,
                () -> service.approvePTO(1L, new PTOApprovalRequest(), "adminuser"));

        assertTrue(ex.getMessage().contains("Only a chief may approve it."));
    }
}
