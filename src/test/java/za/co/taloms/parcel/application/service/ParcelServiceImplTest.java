package za.co.taloms.parcel.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.DuplicateRecordException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.parcel.application.dto.BoundaryPointDto;
import za.co.taloms.parcel.application.dto.ParcelRequest;
import za.co.taloms.parcel.application.dto.ParcelResponse;
import za.co.taloms.parcel.domain.entity.CaptureMode;
import za.co.taloms.parcel.domain.entity.Parcel;
import za.co.taloms.parcel.domain.entity.ParcelBoundary;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.parcel.domain.repository.ParcelBoundaryRepositoryPort;
import za.co.taloms.parcel.domain.repository.ParcelRepositoryPort;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;
import za.co.taloms.traditionalauthority.domain.entity.Village;
import za.co.taloms.traditionalauthority.domain.repository.VillageRepositoryPort;
import org.springframework.context.ApplicationEventPublisher;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParcelServiceImplTest {

    @Mock private ParcelRepositoryPort parcelRepository;
    @Mock private ParcelBoundaryRepositoryPort boundaryRepository;
    @Mock private VillageRepositoryPort villageRepository;
    @Mock private PTORepositoryPort ptoRepository;
    @Mock private ParcelAreaCalculator areaCalculator;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private BoundaryValidationService boundaryValidationService;
    @Mock private za.co.taloms.traditionalauthority.application.service.SpatialBoundaryService spatialBoundaryService;
    @Mock private EntityManager entityManager;

    private ParcelServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ParcelServiceImpl(
                parcelRepository, boundaryRepository, villageRepository,
                ptoRepository, areaCalculator, eventPublisher,
                boundaryValidationService, spatialBoundaryService, entityManager);
    }

    @Test
    void shouldCreateParcelWithCoordinates() {
        var village = Village.builder()
                .id(1L)
                .villageName("Test Village")
                .active(true)
                .build();

        when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
        when(parcelRepository.existsByStandNumberAndVillageId(anyString(), anyLong())).thenReturn(false);

        var boundaries = List.of(
                BoundaryPointDto.builder().sequence(1).latitude(-25.0).longitude(28.0).build(),
                BoundaryPointDto.builder().sequence(2).latitude(-25.01).longitude(28.01).build(),
                BoundaryPointDto.builder().sequence(3).latitude(-25.02).longitude(28.0).build()
        );

        when(areaCalculator.calculateAreaM2(any())).thenReturn(10000.0);
        when(areaCalculator.calculateAreaHectares(any())).thenReturn(1.0);
        when(areaCalculator.calculateCentroid(any())).thenReturn(new Double[]{-25.01, 28.003});
        when(areaCalculator.calculatePerimeterM(any())).thenReturn(500.0);

        var savedParcel = Parcel.builder()
                .id(1L)
                .parcelNumber("PRC-2026-00001")
                .standNumber("ST-001")
                .status(ParcelStatus.AVAILABLE)
                .areaM2(10000.0)
                .areaHectares(1.0)
                .centroidLat(-25.01)
                .centroidLng(28.003)
                .perimeterM(500.0)
                .village(village)
                .captureMode(CaptureMode.MANUAL_TAP)
                .build();

        when(parcelRepository.save(any(Parcel.class))).thenReturn(savedParcel);

        var request = ParcelRequest.builder()
                .standNumber("ST-001")
                .villageId(1L)
                .boundaries(boundaries)
                .captureMode(CaptureMode.MANUAL_TAP)
                .build();

        ParcelResponse response = service.createParcel(request, "testuser");

        assertNotNull(response);
        assertEquals("PRC-2026-00001", response.getParcelNumber());
        assertEquals("ST-001", response.getStandNumber());
        assertEquals(10000.0, response.getAreaM2());
        assertEquals(1.0, response.getAreaHectares());
        assertEquals(500.0, response.getPerimeterM());
        assertEquals(ParcelStatus.AVAILABLE, response.getStatus());
        assertEquals(CaptureMode.MANUAL_TAP, response.getCaptureMode());

        verify(parcelRepository).save(any(Parcel.class));
        verify(boundaryRepository).saveAll(anyList());
        verify(areaCalculator).calculatePerimeterM(any());
    }

    @Test
    void shouldFailWhenVillageNotFound() {
        when(villageRepository.findById(anyLong())).thenReturn(Optional.empty());

        var request = ParcelRequest.builder()
                .standNumber("ST-001")
                .villageId(999L)
                .boundaries(new ArrayList<>())
                .build();

        assertThrows(ResourceNotFoundException.class,
                () -> service.createParcel(request, "testuser"));
    }

    @Test
    void shouldFailWhenStandNumberDuplicate() {
        var village = Village.builder().id(1L).villageName("Test Village").active(true).build();
        when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
        when(parcelRepository.existsByStandNumberAndVillageId("ST-001", 1L)).thenReturn(true);

        var request = ParcelRequest.builder()
                .standNumber("ST-001")
                .villageId(1L)
                .boundaries(new ArrayList<>())
                .build();

        assertThrows(DuplicateRecordException.class,
                () -> service.createParcel(request, "testuser"));
    }

    @Test
    void shouldFailWhenInsufficientBoundaries() {
        var village = Village.builder().id(1L).villageName("Test Village").active(true).build();
        when(villageRepository.findById(1L)).thenReturn(Optional.of(village));

        var request = ParcelRequest.builder()
                .standNumber("ST-001")
                .villageId(1L)
                .boundaries(List.of(
                        BoundaryPointDto.builder().sequence(1).latitude(-25.0).longitude(28.0).build()
                ))
                .build();

        assertThrows(BusinessValidationException.class,
                () -> service.createParcel(request, "testuser"));
    }

    @Test
    void shouldFailWhenParcelOverlapsAnotherVillage() {
        // SpatialBoundaryService is mocked, so wire parseBoundary to a real
        // instance to get genuine geometry in this test.
        var realSpatial = new za.co.taloms.traditionalauthority.application.service.SpatialBoundaryService(
                new com.fasterxml.jackson.databind.ObjectMapper(), null, null);
        when(spatialBoundaryService.parseBoundary(anyString()))
                .thenAnswer(inv -> realSpatial.parseBoundary(inv.getArgument(0)));

        // Village: a ~4km square centred on (-25.00, 28.00)
        var village = Village.builder()
                .id(1L)
                .villageName("Test Village")
                .boundaryJson("[{\"lat\":-24.98,\"lng\":27.98},{\"lat\":-24.98,\"lng\":28.02},"
                        + "{\"lat\":-25.02,\"lng\":28.02},{\"lat\":-25.02,\"lng\":27.98}]")
                .active(true)
                .build();

        // Neighbour overlaps the centre of the village
        var neighbour = Village.builder()
                .id(2L)
                .villageName("Neighbour Village")
                .boundaryJson("[{\"lat\":-24.99,\"lng\":27.99},{\"lat\":-24.99,\"lng\":28.01},"
                        + "{\"lat\":-25.01,\"lng\":28.01},{\"lat\":-25.01,\"lng\":27.99}]")
                .active(true)
                .build();

        when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
        when(parcelRepository.existsByStandNumberAndVillageId(anyString(), anyLong())).thenReturn(false);
        when(villageRepository.findAllActive()).thenReturn(List.of(village, neighbour));

        // Parcel fully inside the village, but sitting in the zone the
        // neighbour also covers — rejected for straddling two villages.
        var boundaries = List.of(
                BoundaryPointDto.builder().sequence(1).latitude(-24.995).longitude(27.995).build(),
                BoundaryPointDto.builder().sequence(2).latitude(-24.995).longitude(28.005).build(),
                BoundaryPointDto.builder().sequence(3).latitude(-25.005).longitude(28.000).build()
        );

        var request = ParcelRequest.builder()
                .standNumber("ST-002")
                .villageId(1L)
                .boundaries(boundaries)
                .build();

        assertThrows(BusinessValidationException.class,
                () -> service.createParcel(request, "testuser"));
    }
}
