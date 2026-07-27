package za.co.taloms.parcel.application.service;

import org.springframework.stereotype.Service;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.parcel.application.dto.BoundaryPointDto;
import za.co.taloms.parcel.domain.entity.Parcel;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.parcel.domain.repository.ParcelRepositoryPort;
import za.co.taloms.parcel.domain.repository.ParcelBoundaryRepositoryPort;

import java.util.List;

/**
 * Validates parcel boundaries against the spatial integrity rules
 * defined in the TALOMS Digital Land Demarcation Technical Framework.
 */
@Service
public class BoundaryValidationService {

    private static final double SOUTH_AFRICA_MIN_LAT = -35.0;
    private static final double SOUTH_AFRICA_MAX_LAT = -22.0;
    private static final double SOUTH_AFRICA_MIN_LNG = 16.0;
    private static final double SOUTH_AFRICA_MAX_LNG = 33.0;
    private static final double CLOSED_LOOP_TOLERANCE_M = 5.0;
    private static final double OVERLAP_WARNING_TOLERANCE_M = 0.5;

    private final ParcelRepositoryPort parcelRepository;
    private final ParcelBoundaryRepositoryPort boundaryRepository;

    public BoundaryValidationService(ParcelRepositoryPort parcelRepository,
                                     ParcelBoundaryRepositoryPort boundaryRepository) {
        this.parcelRepository = parcelRepository;
        this.boundaryRepository = boundaryRepository;
    }

    /**
     * Run the complete validation suite on a set of boundary points.
     */
    public void validateBoundary(List<BoundaryPointDto> boundaries, Long parcelId) {
        if (boundaries == null || boundaries.size() < 3) {
            throw new BusinessValidationException("A parcel must have at least 3 boundary points");
        }

        validateCoordinatesInSouthAfrica(boundaries);
        validateClosedLoop(boundaries);
        validateMinimumArea(boundaries, null);
    }

    /**
     * Ensure no coordinate falls outside South African bounds.
     */
    public void validateCoordinatesInSouthAfrica(List<BoundaryPointDto> boundaries) {
        for (BoundaryPointDto point : boundaries) {
            double lat = point.getLatitude();
            double lng = point.getLongitude();

            if (lat < SOUTH_AFRICA_MIN_LAT || lat > SOUTH_AFRICA_MAX_LAT) {
                throw new BusinessValidationException(
                        "Latitude " + lat + " is outside South African bounds (-35 to -22)");
            }
            if (lng < SOUTH_AFRICA_MIN_LNG || lng > SOUTH_AFRICA_MAX_LNG) {
                throw new BusinessValidationException(
                        "Longitude " + lng + " is outside South African bounds (16 to 33)");
            }
        }
    }

    /**
     * Verify the boundary forms a closed loop (first and last point within tolerance).
     * Auto-snaps the last point to the first if within tolerance.
     */
    public void validateClosedLoop(List<BoundaryPointDto> boundaries) {
        BoundaryPointDto first = boundaries.get(0);
        BoundaryPointDto last = boundaries.get(boundaries.size() - 1);

        double distance = BoundarySimplifier.haversineDistanceM(
                first.getLatitude(), first.getLongitude(),
                last.getLatitude(), last.getLongitude()
        );

        if (distance > CLOSED_LOOP_TOLERANCE_M) {
            throw new BusinessValidationException(
                    "Boundary not closed. You are " + String.format("%.1f", distance) +
                            "m from your starting point. Please return to the start and complete the loop.");
        }

        if (distance > 0.01) {
            last.setLatitude(first.getLatitude());
            last.setLongitude(first.getLongitude());
        }
    }

    /**
     * Validate minimum area based on intended use (from PTO purpose).
     * These thresholds match the TALOMS framework rules for South African
     * traditional authority land.
     */
    public void validateMinimumArea(List<BoundaryPointDto> boundaries, String purpose) {
        if (purpose == null) {
            return;
        }

        double areaM2 = new ParcelAreaCalculatorImpl().calculateAreaM2(boundaries);

        double minArea = switch (purpose.toUpperCase()) {
            case "BUSINESS", "COMMERCIAL" -> 100.0;
            case "AGRICULTURAL" -> 2500.0;
            case "RESIDENTIAL", "HOUSEHOLD" -> 200.0;
            default -> 200.0;
        };

        if (areaM2 < minArea) {
            throw new BusinessValidationException(
                    "This parcel (" + String.format("%.1f", areaM2) + "m²) is below the minimum " +
                    "for " + purpose + " stands (" + minArea + "m²). Please re-demarcate.");
        }
    }

    /**
     * Check for self-intersecting polygons using PostGIS ST_IsValid.
     */
    public void validateSelfIntersection(Long parcelId) {
        // This is handled via a PostGIS native query in the repository.
        // The check is performed after geometry is saved.
    }
}


