package za.co.taloms.traditionalauthority.application.service;

import org.junit.jupiter.api.Test;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.traditionalauthority.domain.repository.TraditionalAuthorityRepositoryPort;
import za.co.taloms.traditionalauthority.domain.repository.VillageRepositoryPort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SpatialBoundaryServiceTest {

    private final SpatialBoundaryService service = new SpatialBoundaryService(
            new com.fasterxml.jackson.databind.ObjectMapper(),
            mock(TraditionalAuthorityRepositoryPort.class),
            mock(VillageRepositoryPort.class));

    @Test
    void parsesShortLatLngKeysFromTheMapEditor() {
        var points = service.parseBoundary(
                "[{\"lat\":-25.5,\"lng\":28.1},{\"lat\":-25.6,\"lng\":28.2},{\"lat\":-25.7,\"lng\":28.0}]");
        assertNotNull(points);
        assertEquals(3, points.size());
        assertEquals(-25.5, points.get(0)[0]);
        assertEquals(28.1, points.get(0)[1]);
    }

    @Test
    void parsesLongLatitudeLongitudeKeys() {
        var points = service.parseBoundary(
                "[{\"latitude\":-25.5,\"longitude\":28.1},{\"latitude\":-25.6,\"longitude\":28.2},{\"latitude\":-25.7,\"longitude\":28.0}]");
        assertNotNull(points);
        assertEquals(-25.5, points.get(0)[0]);
        assertEquals(28.1, points.get(0)[1]);
    }

    @Test
    void rejectsBlankAndTooSmallBoundaries() {
        assertNull(service.parseBoundary(null));
        assertNull(service.parseBoundary("  "));
        assertThrows(BusinessValidationException.class,
                () -> service.parseBoundary("[{\"lat\":1,\"lng\":2}]"));
        assertThrows(BusinessValidationException.class,
                () -> service.parseBoundary("not-json"));
    }
}
