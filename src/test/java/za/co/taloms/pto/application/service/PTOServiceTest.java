package za.co.taloms.pto.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.document.application.service.DocumentService;
import za.co.taloms.document.domain.entity.DocumentType;
import za.co.taloms.document.domain.entity.EntityType;
import za.co.taloms.parcel.domain.entity.Parcel;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.parcel.domain.repository.ParcelRepositoryPort;
import za.co.taloms.pto.application.dto.PTOApprovalRequest;
import za.co.taloms.pto.application.dto.PTORequest;
import za.co.taloms.pto.application.dto.PTORevokeRequest;
import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.entity.PTOPurpose;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;
import za.co.taloms.pto.domain.repository.PTOApprovalSignatureRepositoryPort;
import za.co.taloms.traditionalauthority.domain.entity.TraditionalAuthority;
import za.co.taloms.traditionalauthority.domain.entity.Village;
import za.co.taloms.traditionalauthority.domain.repository.TraditionalAuthorityRepositoryPort;
import za.co.taloms.traditionalauthority.domain.repository.VillageRepositoryPort;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PTOServiceTest {

    @Mock private PTORepositoryPort ptoRepository;
    @Mock private ParcelRepositoryPort parcelRepository;
    @Mock private PTONumberGenerator numberGenerator;
    @Mock private TraditionalAuthorityRepositoryPort authorityRepository;
    @Mock private VillageRepositoryPort villageRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private DocumentService documentService;
    @Mock private PTOApprovalSignatureRepositoryPort signatureRepository;

    private PTOServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PTOServiceImpl(
                ptoRepository, parcelRepository, numberGenerator,
                authorityRepository, villageRepository, eventPublisher, documentService, signatureRepository);

        lenient().when(numberGenerator.generate()).thenReturn("PTO-2026-00001");
        lenient().when(ptoRepository.existsByPtoNumber(anyString())).thenReturn(false);
    }

    private PTORequest buildValidRequest(Long parcelId, Long authorityId, Long villageId) {
        return PTORequest.builder()
                .ptoHolderName("Sipho Dlamini")
                .idNumber("9001010000001")
                .contactPhone("+27820000000")
                .purpose("RESIDENTIAL")
                .issueDate(java.time.LocalDate.of(2026, 7, 25))
                .parcelId(parcelId)
                .standNumber("10")
                .parcelNumber("PAR-001")
                .villageId(villageId)
                .traditionalAuthorityId(authorityId)
                .allocatedBy("Headman Mahlangu")
                .allocationDate(java.time.LocalDate.of(2026, 7, 20))
                .standArea(300.0)
                .surveyReference("SG 8977/1999")
                .taRecommendationRef("TA-REC-001")
                .build();
    }

    private Parcel buildParcel(Long id, Village village, String standNumber, ParcelStatus status) {
        Parcel parcel = new Parcel();
        parcel.setId(id);
        parcel.setVillage(village);
        parcel.setStandNumber(standNumber);
        parcel.setParcelNumber("PAR-001");
        parcel.setStatus(status);
        return parcel;
    }

    private Village buildVillage(Long id, Long authorityId) {
        Village village = new Village();
        village.setId(id);
        village.setVillageName("Ga-Mitchel");
        TraditionalAuthority ta = new TraditionalAuthority();
        ta.setId(authorityId);
        village.setTraditionalAuthority(ta);
        return village;
    }

    @Test
    void shouldCreatePTOWithMetadata() {
        Long authorityId = 1L;
        Long villageId = 2L;
        Long parcelId = 3L;

        var authority = new TraditionalAuthority();
        authority.setId(authorityId);
        authority.setAuthorityName("Rambuda TA");

        var village = buildVillage(villageId, authorityId);
        var parcel = buildParcel(parcelId, village, "10", ParcelStatus.AVAILABLE);

        when(authorityRepository.findById(authorityId)).thenReturn(Optional.of(authority));
        when(villageRepository.findById(villageId)).thenReturn(Optional.of(village));
        when(parcelRepository.findById(parcelId)).thenReturn(Optional.of(parcel));
        when(ptoRepository.save(any(PTO.class))).thenAnswer(i -> i.getArgument(0));

        var request = buildValidRequest(parcelId, authorityId, villageId);
        var response = service.createPTO(request, "data.capturer");

        assertNotNull(response);
        assertEquals("PTO-2026-00001", response.getPtoNumber());
        assertEquals("Sipho Dlamini", response.getPtoHolderName());
        assertEquals("Headman Mahlangu", response.getAllocatedBy());
        assertEquals(300.0, response.getStandArea());
        assertEquals("SG 8977/1999", response.getSurveyReference());
        assertEquals(PTOStatus.PENDING, response.getStatus());
    }

    @Test
    void shouldRecreateWhenPtoNumberExists() {
        Long authorityId = 1L;
        Long villageId = 2L;
        Long parcelId = 3L;

        var authority = new TraditionalAuthority();
        authority.setId(authorityId);
        authority.setAuthorityName("Rambuda TA");

        var village = buildVillage(villageId, authorityId);
        var parcel = buildParcel(parcelId, village, "10", ParcelStatus.AVAILABLE);

        when(authorityRepository.findById(authorityId)).thenReturn(Optional.of(authority));
        when(villageRepository.findById(villageId)).thenReturn(Optional.of(village));
        when(parcelRepository.findById(parcelId)).thenReturn(Optional.of(parcel));
        when(numberGenerator.generate()).thenReturn("PTO-2026-00001", "PTO-2026-00002");
        when(ptoRepository.existsByPtoNumber("PTO-2026-00001")).thenReturn(true);
        when(ptoRepository.existsByPtoNumber("PTO-2026-00002")).thenReturn(false);
        when(ptoRepository.save(any(PTO.class))).thenAnswer(i -> i.getArgument(0));

        var request = buildValidRequest(parcelId, authorityId, villageId);
        var response = service.createPTO(request, "data.capturer");

        assertEquals("PTO-2026-00002", response.getPtoNumber());
    }

    @Test
    void shouldFailApprovalWhenDocumentsMissing() {
        Long ptoId = 1L;
        PTO pto = new PTO();
        pto.setId(ptoId);
        pto.setStatus(PTOStatus.PENDING);
        var village = buildVillage(2L, 1L);
        pto.setParcel(buildParcel(3L, village, "10", ParcelStatus.AVAILABLE));

        when(ptoRepository.findById(ptoId)).thenReturn(Optional.of(pto));
        when(documentService.getMissingRequiredDocumentTypes(EntityType.PTO, ptoId))
                .thenReturn(List.of(DocumentType.TA_ALLOCATION_LETTER, DocumentType.SITE_SKETCH));

        var ex = assertThrows(BusinessValidationException.class,
                () -> service.approvePTO(ptoId, new PTOApprovalRequest(), "admin"));

        assertTrue(ex.getMessage().contains("required documents missing"));
    }

    @Test
    void shouldApproveWhenDocumentsPresent() {
        Long ptoId = 1L;
        PTO pto = new PTO();
        pto.setId(ptoId);
        pto.setStatus(PTOStatus.PENDING);
        pto.setPurpose(PTOPurpose.RESIDENTIAL);
        var village = buildVillage(2L, 1L);
        pto.setParcel(buildParcel(3L, village, "10", ParcelStatus.AVAILABLE));

        when(ptoRepository.findById(ptoId)).thenReturn(Optional.of(pto));
        when(documentService.getMissingRequiredDocumentTypes(EntityType.PTO, ptoId))
                .thenReturn(List.of());
        when(ptoRepository.save(any(PTO.class))).thenAnswer(i -> i.getArgument(0));
        when(parcelRepository.save(any(Parcel.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.approvePTO(ptoId, new PTOApprovalRequest(), "admin");

        assertEquals(PTOStatus.ACTIVE, response.getStatus());
        assertEquals("admin", response.getApprovedBy());
    }

    @Test
    void shouldFailWhenParcelNotAvailable() {
        Long authorityId = 1L;
        Long villageId = 2L;
        Long parcelId = 3L;

        var authority = new TraditionalAuthority();
        authority.setId(authorityId);
        var village = buildVillage(villageId, authorityId);
        var parcel = buildParcel(parcelId, village, "10", ParcelStatus.ALLOCATED);

        when(authorityRepository.findById(authorityId)).thenReturn(Optional.of(authority));
        when(villageRepository.findById(villageId)).thenReturn(Optional.of(village));
        when(parcelRepository.findById(parcelId)).thenReturn(Optional.of(parcel));

        var request = buildValidRequest(parcelId, authorityId, villageId);
        var ex = assertThrows(BusinessValidationException.class,
                () -> service.createPTO(request, "land.officer"));

        assertTrue(ex.getMessage().contains("not available"));
    }

    @Test
    void shouldFailWhenVillageDoesNotBelongToAuthority() {
        Long authorityId = 1L;
        Long wrongAuthorityId = 99L;
        Long villageId = 2L;
        Long parcelId = 3L;

        var authority = new TraditionalAuthority();
        authority.setId(authorityId);
        var village = buildVillage(villageId, wrongAuthorityId);
        var parcel = buildParcel(parcelId, village, "10", ParcelStatus.AVAILABLE);

        when(authorityRepository.findById(authorityId)).thenReturn(Optional.of(authority));
        when(villageRepository.findById(villageId)).thenReturn(Optional.of(village));

        var request = buildValidRequest(parcelId, authorityId, villageId);
        var ex = assertThrows(BusinessValidationException.class,
                () -> service.createPTO(request, "land.officer"));

        assertTrue(ex.getMessage().contains("does not belong to"));
    }

    @Test
    void shouldSetCommunityResolutionForBusiness() {
        Long authorityId = 1L;
        Long villageId = 2L;
        Long parcelId = 3L;

        var authority = new TraditionalAuthority();
        authority.setId(authorityId);
        var village = buildVillage(villageId, authorityId);
        var parcel = buildParcel(parcelId, village, "10", ParcelStatus.AVAILABLE);

        when(authorityRepository.findById(authorityId)).thenReturn(Optional.of(authority));
        when(villageRepository.findById(villageId)).thenReturn(Optional.of(village));
        when(parcelRepository.findById(parcelId)).thenReturn(Optional.of(parcel));
        when(ptoRepository.save(any(PTO.class))).thenAnswer(i -> i.getArgument(0));

        var request = PTORequest.builder()
                .ptoHolderName("Thabo Mbeki")
                .idNumber("8001010000002")
                .purpose("BUSINESS")
                .issueDate(java.time.LocalDate.of(2026, 7, 25))
                .parcelId(parcelId)
                .standNumber("10")
                .parcelNumber("PAR-001")
                .villageId(villageId)
                .traditionalAuthorityId(authorityId)
                .communityResolutionRequired(true)
                .build();

        var response = service.createPTO(request, "data.capturer");
        assertTrue(response.getCommunityResolutionRequired());
    }
}
