package za.co.taloms.parcel.application.service;

import org.springframework.stereotype.Service;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.parcel.application.dto.BoundaryPointDto;
import java.util.List;

@Service
public class ParcelAreaCalculatorImpl implements ParcelAreaCalculator {

    @Override
    public Double calculateAreaM2(List<BoundaryPointDto> boundaries) {
        if (boundaries == null || boundaries.size() < 3) {
            throw new BusinessValidationException("Polygon must have at least 3 points");
        }

        // Shoelace formula for area calculation in square meters
        // Using WGS84 coordinates approximated to meters at SA latitude (~30°S)
        // Scale factor: 1 degree ≈ 111,320 meters (at equator, adjusted for latitude)
        double latAvg = boundaries.stream()
                .mapToDouble(BoundaryPointDto::getLatitude)
                .average()
                .orElse(0.0);

        // Mercator projection scale factor
        double latRad = Math.toRadians(latAvg);
        double scale = 111320 * Math.cos(latRad);

        double sum1 = 0.0;
        double sum2 = 0.0;
        int n = boundaries.size();

        for (int i = 0; i < n; i++) {
            BoundaryPointDto p1 = boundaries.get(i);
            BoundaryPointDto p2 = boundaries.get((i + 1) % n);

            double x1 = p1.getLongitude() * scale;
            double y1 = p1.getLatitude() * scale;
            double x2 = p2.getLongitude() * scale;
            double y2 = p2.getLatitude() * scale;

            sum1 += x1 * y2;
            sum2 += x2 * y1;
        }

        double area = Math.abs(sum1 - sum2) / 2.0;
        return Math.round(area * 100.0) / 100.0; // Round to 2 decimal places
    }

    @Override
    public Double calculateAreaHectares(List<BoundaryPointDto> boundaries) {
        double areaM2 = calculateAreaM2(boundaries);
        return Math.round((areaM2 / 10000.0) * 10000.0) / 10000.0; // Round to 4 decimal places
    }

    @Override
    public Double[] calculateCentroid(List<BoundaryPointDto> boundaries) {
        if (boundaries == null || boundaries.isEmpty()) {
            return new Double[]{0.0, 0.0};
        }

        double latSum = 0.0;
        double lngSum = 0.0;
        int n = boundaries.size();

        for (BoundaryPointDto point : boundaries) {
            latSum += point.getLatitude();
            lngSum += point.getLongitude();
        }

        return new Double[]{latSum / n, lngSum / n};
    }
}