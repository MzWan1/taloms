package za.co.taloms.company.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.company.application.dto.PorVerificationResponse;
import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.traditionalauthority.domain.entity.Village;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;
import za.co.taloms.resident.domain.entity.Resident;
import za.co.taloms.resident.domain.repository.ResidentRepositoryPort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProofOfResidenceServiceImplTest {

    @Mock private ResidentRepositoryPort residentRepository;
    @Mock private PTORepositoryPort ptoRepository;
    private ProofOfResidenceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProofOfResidenceServiceImpl(residentRepository, ptoRepository);
    }

    private Village village(String name) {
        Village v = new Village();
        v.setVillageName(name);
        return v;
    }

    @Test
    void validIdWithValidPorReturnsVerified() {
        String id = "9001015000085";
        when(ptoRepository.findByIdNumberAndStatus(id, PTOStatus.ACTIVE))
                .thenReturn(List.of(PTO.builder()
                        .idNumber(id).ptoNumber("PTO-001").ptoHolderName("Test Person")
                        .status(PTOStatus.ACTIVE).village(village("Elandskloof"))
                        .traditionalAuthority(null)
                        .issueDate(LocalDate.of(2024, 1, 1)).expiryDate(LocalDate.of(2026, 12, 31))
                        .deletedAt(null).build()));
        when(residentRepository.findByIdNumber(id))
                .thenReturn(Optional.of(Resident.builder().idNumber(id).fullName("Test Person").build()));

        PorVerificationResponse resp = service.verify(id);

        assertTrue(resp.isVerified());
        assertEquals("Test Person", resp.getResidentName());
        assertEquals("900101****085", resp.getMaskedIdNumber());
        assertEquals("PTO-001", resp.getProofOfResidenceNumber());
        assertEquals("ACTIVE", resp.getProofStatus());
        assertEquals("Elandskloof", resp.getVillageName());
        assertNotNull(resp.getVerifiedAt());
    }

    @Test
    void validIdWithoutValidPorReturnsNotFound() {
        String id = "9001015000085";
        when(ptoRepository.findByIdNumberAndStatus(id, PTOStatus.ACTIVE)).thenReturn(List.of());

        PorVerificationResponse resp = service.verify(id);

        assertFalse(resp.isVerified());
        assertEquals("NOT_FOUND", resp.getReason());
        assertNull(resp.getResidentName());
        assertNull(resp.getMaskedIdNumber());
        assertNull(resp.getProofOfResidenceNumber());
    }

    @Test
    void unknownIdReturnsSameNotFoundAsNoPor() {
        String unknownId = "1234567890123";
        when(ptoRepository.findByIdNumberAndStatus(unknownId, PTOStatus.ACTIVE)).thenReturn(List.of());

        PorVerificationResponse resp = service.verify(unknownId);

        assertFalse(resp.isVerified());
        assertEquals("NOT_FOUND", resp.getReason());
        assertNull(resp.getResidentName());
        assertNull(resp.getMaskedIdNumber());
    }

    @Test
    void malformedIdThrowsValidationError() {
        assertThrows(BusinessValidationException.class, () -> service.verify("12345"));
        assertThrows(BusinessValidationException.class, () -> service.verify("abcdefghijk"));
        assertThrows(BusinessValidationException.class, () -> service.verify(""));
        assertThrows(BusinessValidationException.class, () -> service.verify(null));
    }
}
