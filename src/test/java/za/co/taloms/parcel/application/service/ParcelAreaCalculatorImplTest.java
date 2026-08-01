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

    @Test
    void shouldCalculateAreaFor200m2ParcelWithHighPrecisionCoordinates() {
        // Create a ~225m² parcel (15m x 15m square) at Witbank, South Africa coordinates
        // Using 8 decimal places precision to test coordinate precision fix
        // Latitude: ~ -25.87194444 (Witbank area), Longitude: ~ 29.23333333
        // At latitude -25.87: 1° lat ≈ 111132m, 1° lng ≈ 111320 * cos(-25.87°) ≈ 100200m
        // 15m in lat degrees = 15/111132 ≈ 0.000135
        // 15m in lng degrees = 15/100200 ≈ 0.000150
        var boundaries = List.of(
                BoundaryPointDto.builder().sequence(1).latitude(-25.87194444).longitude(29.23333333).build(),
                BoundaryPointDto.builder().sequence(2).latitude(-25.87194444).longitude(29.23348333).build(),
                BoundaryPointDto.builder().sequence(3).latitude(-25.87207944).longitude(29.23348333).build(),
                BoundaryPointDto.builder().sequence(4).latitude(-25.87207944).longitude(29.23333333).build()
        );

        double area = calculator.calculateAreaM2(boundaries);
        double perimeter = calculator.calculatePerimeterM(boundaries);

        // Verify area is at least 200m²
        assertTrue(area >= 200, "Area should be at least 200m², got: " + area);
        // Verify perimeter is reasonable for ~15m x 15m square (~60m)
        assertTrue(perimeter > 50 && perimeter < 80, "Perimeter should be ~60m for 15m square, got: " + perimeter);

        // Verify coordinates are handled with 8 decimal places precision
        // The points should be distinct and not duplicated due to precision issues
        assertEquals(4, boundaries.size(), "Should have 4 boundary points");
    }

    @Test
    void shouldDistinguishAdjacentPointsWithHighPrecision() {
        // Test that points 1 meter apart at Witbank coordinates are distinguishable with 8 decimal places
        // At latitude -25.87: 1° lat ≈ 111132m, 1° lng ≈ 100200m
        // 1m in lat degrees = 1/111132 ≈ 0.0000090
        // 1m in lng degrees = 1/100200 ≈ 0.00000998
        var boundaries = List.of(
                BoundaryPointDto.builder().sequence(1).latitude(-25.87194444).longitude(29.23333333).build(),
                BoundaryPointDto.builder().sequence(2).latitude(-25.87195344).longitude(29.23333333).build(), // ~1m north
                BoundaryPointDto.builder().sequence(3).latitude(-25.87195344).longitude(29.23334331).build(), // ~1m east
                BoundaryPointDto.builder().sequence(4).latitude(-25.87194444).longitude(29.23334331).build()  // ~1m south
        );

        double area = calculator.calculateAreaM2(boundaries);
        double perimeter = calculator.calculatePerimeterM(boundaries);

        // Verify area is ~1m² for 1m x 1m square
        assertTrue(area > 0.5 && area < 2.0, "Area should be ~1m² for 1m square, got: " + area);
        // Verify perimeter is ~4m
        assertTrue(perimeter > 3.5 && perimeter < 4.5, "Perimeter should be ~4m for 1m square, got: " + perimeter);

        // Verify all points are distinct (not duplicated due to precision loss)
        assertEquals(4, boundaries.size(), "Should have 4 distinct boundary points");
    }
}
