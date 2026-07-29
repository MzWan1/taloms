package za.co.taloms.parcel.application.service;

import org.junit.jupiter.api.Test;
import za.co.taloms.parcel.application.dto.BoundaryPointDto;
import za.co.taloms.common.BusinessValidationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParcelAreaCalculatorImplTest {

    private final ParcelAreaCalculatorImpl calculator = new ParcelAreaCalculatorImpl();

    @Test
    void shouldCalculateAreaForSmallSquare() {
        var boundaries = List.of(
                BoundaryPointDto.builder().sequence(1).latitude(-25.0).longitude(28.0).build(),
                BoundaryPointDto.builder().sequence(2).latitude(-25.0).longitude(28.01).build(),
                BoundaryPointDto.builder().sequence(3).latitude(-25.01).longitude(28.01).build(),
                BoundaryPointDto.builder().sequence(4).latitude(-25.01).longitude(28.0).build()
        );

        double area = calculator.calculateAreaM2(boundaries);
        assertTrue(area > 0, "Area should be positive");
        assertTrue(area > 100000, "Area should be roughly 123km² for this 0.1° square near SA");
    }

    @Test
    void shouldCalculatePerimeterForSquare() {
        var boundaries = List.of(
                BoundaryPointDto.builder().sequence(1).latitude(-25.0).longitude(28.0).build(),
                BoundaryPointDto.builder().sequence(2).latitude(-25.0).longitude(28.01).build(),
                BoundaryPointDto.builder().sequence(3).latitude(-25.01).longitude(28.01).build(),
                BoundaryPointDto.builder().sequence(4).latitude(-25.01).longitude(28.0).build()
        );

        double perimeter = calculator.calculatePerimeterM(boundaries);
        assertTrue(perimeter > 0, "Perimeter should be positive");
        assertTrue(perimeter > 4000 && perimeter < 5000, "Perimeter should be roughly 4.2km for this 0.1° square near SA");
    }

    @Test
    void shouldCalculateCentroid() {
        var boundaries = List.of(
                BoundaryPointDto.builder().sequence(1).latitude(-25.0).longitude(28.0).build(),
                BoundaryPointDto.builder().sequence(2).latitude(-25.0).longitude(28.01).build(),
                BoundaryPointDto.builder().sequence(3).latitude(-25.01).longitude(28.01).build(),
                BoundaryPointDto.builder().sequence(4).latitude(-25.01).longitude(28.0).build()
        );

        Double[] centroid = calculator.calculateCentroid(boundaries);
        System.out.println("Centroid: [" + centroid[0] + ", " + centroid[1] + "]");
        assertNotNull(centroid);
        assertEquals(2, centroid.length);
        assertFalse(centroid[0].isNaN() || centroid[1].isNaN(), "Centroid should not be NaN");
        assertTrue(centroid[0] > -50 && centroid[0] < 50, "Latitude should be a valid WGS84 value");
        assertTrue(centroid[1] > -180 && centroid[1] < 180, "Longitude should be a valid WGS84 value");
    }

    @Test
    void shouldConvertToHectares() {
        var boundaries = List.of(
                BoundaryPointDto.builder().sequence(1).latitude(-25.0).longitude(28.0).build(),
                BoundaryPointDto.builder().sequence(2).latitude(-25.0).longitude(28.01).build(),
                BoundaryPointDto.builder().sequence(3).latitude(-25.01).longitude(28.01).build(),
                BoundaryPointDto.builder().sequence(4).latitude(-25.01).longitude(28.0).build()
        );

        double hectares = calculator.calculateAreaHectares(boundaries);
        double m2 = calculator.calculateAreaM2(boundaries);
        assertEquals(m2 / 10000.0, hectares, 0.01);
    }

    @Test
    void shouldThrowOnInsufficientPoints() {
        var boundaries = List.of(
                BoundaryPointDto.builder().sequence(1).latitude(-25.0).longitude(28.0).build(),
                BoundaryPointDto.builder().sequence(2).latitude(-25.0).longitude(28.01).build()
        );

        assertThrows(BusinessValidationException.class,
                () -> calculator.calculateAreaM2(boundaries));
    }
}
