package za.co.taloms.parcel.application.service;

import org.springframework.stereotype.Service;
import za.co.taloms.parcel.application.dto.BoundaryPointDto;
import za.co.taloms.common.BusinessValidationException;
import java.util.ArrayList;  // ADD THIS IMPORT
import java.util.List;

@Service
public class ParcelAreaCalculatorImpl implements ParcelAreaCalculator {

    private static final double HAVERSINE_R = 6371000.0;
    private static final double METERS_PER_DEG_LAT = 111132.0;
    private static final double METERS_PER_DEG_LNG = 111320.0;

    @Override
    public Double calculateAreaM2(List<BoundaryPointDto> boundaries) {
        if (boundaries == null || boundaries.size() < 3) {
            throw new BusinessValidationException("Polygon must have at least 3 points");
        }

        // Remove duplicate points if the polygon is closed
        List<BoundaryPointDto> cleanPoints = cleanBoundaryPoints(boundaries);

        if (cleanPoints.size() < 3) {
            throw new BusinessValidationException("Polygon must have at least 3 unique points");
        }

        // Use the centroid for the projection reference point
        double latAvg = cleanPoints.stream()
                .mapToDouble(BoundaryPointDto::getLatitude)
                .average()
                .orElse(0.0);
        double lngAvg = cleanPoints.stream()
                .mapToDouble(BoundaryPointDto::getLongitude)
                .average()
                .orElse(0.0);

        double latRad = Math.toRadians(latAvg);
        double metersPerDegLat = METERS_PER_DEG_LAT;
        double metersPerDegLng = METERS_PER_DEG_LNG * Math.cos(latRad);

        // Shoelace formula in projected coordinates
        // The points are in consecutive order and the polygon is closed
        double sum = 0.0;
        int n = cleanPoints.size();

        for (int i = 0; i < n; i++) {
            BoundaryPointDto p1 = cleanPoints.get(i);
            BoundaryPointDto p2 = cleanPoints.get((i + 1) % n);

            double x1 = (p1.getLongitude() - lngAvg) * metersPerDegLng;
            double y1 = (p1.getLatitude() - latAvg) * metersPerDegLat;
            double x2 = (p2.getLongitude() - lngAvg) * metersPerDegLng;
            double y2 = (p2.getLatitude() - latAvg) * metersPerDegLat;

            sum += (x1 * y2 - x2 * y1);
        }

        double area = Math.abs(sum) / 2.0;
        return Math.round(area * 100.0) / 100.0;
    }

    @Override
    public Double calculatePerimeterM(List<BoundaryPointDto> boundaries) {
        if (boundaries == null || boundaries.size() < 2) {
            return 0.0;
        }

        List<BoundaryPointDto> cleanPoints = cleanBoundaryPoints(boundaries);

        if (cleanPoints.size() < 2) {
            return 0.0;
        }

        double perimeter = 0.0;
        int n = cleanPoints.size();

        for (int i = 0; i < n; i++) {
            BoundaryPointDto p1 = cleanPoints.get(i);
            BoundaryPointDto p2 = cleanPoints.get((i + 1) % n);
            perimeter += haversineDistanceM(
                    p1.getLatitude(), p1.getLongitude(),
                    p2.getLatitude(), p2.getLongitude()
            );
        }

        return Math.round(perimeter * 100.0) / 100.0;
    }

    @Override
    public Double calculateAreaHectares(List<BoundaryPointDto> boundaries) {
        double areaM2 = calculateAreaM2(boundaries);
        return Math.round((areaM2 / 10000.0) * 10000.0) / 10000.0;
    }

    @Override
    public Double[] calculateCentroid(List<BoundaryPointDto> boundaries) {
        if (boundaries == null || boundaries.isEmpty()) {
            return new Double[]{0.0, 0.0};
        }

        List<BoundaryPointDto> cleanPoints = cleanBoundaryPoints(boundaries);

        if (cleanPoints.size() < 3) {
            double latAvg = cleanPoints.stream()
                    .mapToDouble(BoundaryPointDto::getLatitude)
                    .average()
                    .orElse(0.0);
            double lngAvg = cleanPoints.stream()
                    .mapToDouble(BoundaryPointDto::getLongitude)
                    .average()
                    .orElse(0.0);
            return new Double[]{latAvg, lngAvg};
        }

        double latAvg = cleanPoints.stream()
                .mapToDouble(BoundaryPointDto::getLatitude)
                .average()
                .orElse(0.0);
        double lngAvg = cleanPoints.stream()
                .mapToDouble(BoundaryPointDto::getLongitude)
                .average()
                .orElse(0.0);

        double latRad = Math.toRadians(latAvg);
        double metersPerDegLat = METERS_PER_DEG_LAT;
        double metersPerDegLng = METERS_PER_DEG_LNG * Math.cos(latRad);

        double cx = 0.0;
        double cy = 0.0;
        double signedArea = 0.0;
        int n = cleanPoints.size();

        for (int i = 0; i < n; i++) {
            BoundaryPointDto p1 = cleanPoints.get(i);
            BoundaryPointDto p2 = cleanPoints.get((i + 1) % n);

            double x1 = (p1.getLongitude() - lngAvg) * metersPerDegLng;
            double y1 = (p1.getLatitude() - latAvg) * metersPerDegLat;
            double x2 = (p2.getLongitude() - lngAvg) * metersPerDegLng;
            double y2 = (p2.getLatitude() - latAvg) * metersPerDegLat;

            double cross = x1 * y2 - x2 * y1;
            signedArea += cross;
            cx += (x1 + x2) * cross;
            cy += (y1 + y2) * cross;
        }

        signedArea /= 2.0;

        if (Math.abs(signedArea) < 1e-6) {
            return new Double[]{
                    cleanPoints.stream().mapToDouble(BoundaryPointDto::getLatitude).average().orElse(0.0),
                    cleanPoints.stream().mapToDouble(BoundaryPointDto::getLongitude).average().orElse(0.0)
            };
        }

        cx /= (6.0 * signedArea);
        cy /= (6.0 * signedArea);

        double centroidLat = latAvg + (cy / metersPerDegLat);
        double centroidLng = lngAvg + (cx / metersPerDegLng);

        return new Double[]{centroidLat, centroidLng};
    }

    /**
     * Clean the boundary points by removing duplicate consecutive points.
     * This handles the case where the polygon is closed (first point = last point).
     */
    private List<BoundaryPointDto> cleanBoundaryPoints(List<BoundaryPointDto> boundaries) {
        if (boundaries == null || boundaries.isEmpty()) {
            return boundaries;
        }

        List<BoundaryPointDto> result = new ArrayList<>();

        for (BoundaryPointDto point : boundaries) {
            if (point != null && point.getLatitude() != null && point.getLongitude() != null) {
                if (result.isEmpty()) {
                    result.add(point);
                } else {
                    BoundaryPointDto last = result.get(result.size() - 1);
                    // Check if this point is different from the last (within 0.000001 degrees ~ 0.1m)
                    if (Math.abs(point.getLatitude() - last.getLatitude()) > 0.000001 ||
                            Math.abs(point.getLongitude() - last.getLongitude()) > 0.000001) {
                        result.add(point);
                    }
                }
            }
        }

        // If we have a closed polygon (first == last) and more than 3 points, remove the last duplicate
        if (result.size() > 3) {
            BoundaryPointDto first = result.get(0);
            BoundaryPointDto last = result.get(result.size() - 1);
            double distance = haversineDistanceM(
                    first.getLatitude(), first.getLongitude(),
                    last.getLatitude(), last.getLongitude()
            );
            if (distance < 0.01) {
                // Remove the duplicate last point for calculation purposes
                result.remove(result.size() - 1);
            }
        }

        return result;
    }

    private static double haversineDistanceM(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return HAVERSINE_R * c;
    }
}