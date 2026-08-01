package za.co.taloms.parcel.application.service;

import za.co.taloms.parcel.application.dto.BoundaryPointDto;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Utility for simplifying GPS boundary traces using the Douglas-Peucker algorithm.
 *
 * IMPORTANT: This algorithm preserves the ORDER of points while reducing the total count.
 * The points are in consecutive order as captured by GPS.
 */
public class BoundarySimplifier {

    /**
     * Simplify a list of boundary points using Douglas-Peucker.
     * The points MUST be in consecutive order (the order they were captured).
     *
     * @param points     the original boundary points in consecutive order
     * @param toleranceM tolerance in metres — points closer than this to the line
     *                   between start and end are discarded
     * @return simplified list of boundary points (preserving order)
     */
    public static List<BoundaryPointDto> simplify(List<BoundaryPointDto> points, double toleranceM) {
        if (points == null || points.size() < 3) {
            return points;
        }

        // Remove null points
        List<BoundaryPointDto> validPoints = new ArrayList<>();
        for (BoundaryPointDto p : points) {
            if (p != null && p.getLatitude() != null && p.getLongitude() != null) {
                validPoints.add(p);
            }
        }

        if (validPoints.size() < 3) {
            return validPoints;
        }

        // Check if the boundary is already closed (first point = last point)
        boolean isClosed = false;
        if (validPoints.size() > 1) {
            BoundaryPointDto first = validPoints.get(0);
            BoundaryPointDto last = validPoints.get(validPoints.size() - 1);
            double distance = haversineDistanceM(
                    first.getLatitude(), first.getLongitude(),
                    last.getLatitude(), last.getLongitude()
            );
            isClosed = distance < 1.0;
        }

        // If closed, remove the duplicate last point for simplification
        List<BoundaryPointDto> workingPoints = new ArrayList<>(validPoints);
        if (isClosed && workingPoints.size() > 3) {
            // Remove the last point (duplicate of first)
            workingPoints.remove(workingPoints.size() - 1);
        }

        if (workingPoints.size() < 3) {
            return validPoints;
        }

        // Run Douglas-Peucker on the working points (preserving order)
        TreeSet<Integer> keepIndices = new TreeSet<>();
        douglasPeucker(workingPoints, 0, workingPoints.size() - 1, toleranceM, keepIndices);

        // Build the result in order
        List<BoundaryPointDto> result = new ArrayList<>();
        for (int idx : keepIndices) {
            result.add(workingPoints.get(idx));
        }

        // If the original was closed and we have at least 3 points, re-close it
        if (isClosed && result.size() >= 3) {
            BoundaryPointDto first = result.get(0);
            BoundaryPointDto closurePoint = new BoundaryPointDto();
            closurePoint.setLatitude(first.getLatitude());
            closurePoint.setLongitude(first.getLongitude());
            closurePoint.setSequence(result.size() + 1);
            result.add(closurePoint);
        }

        return result;
    }

    private static void douglasPeucker(List<BoundaryPointDto> points,
                                       int start, int end,
                                       double toleranceM,
                                       TreeSet<Integer> keepIndices) {
        keepIndices.add(start);
        keepIndices.add(end);

        double maxDist = 0.0;
        int maxIndex = start;

        BoundaryPointDto p1 = points.get(start);
        BoundaryPointDto p2 = points.get(end);

        for (int i = start + 1; i < end; i++) {
            double dist = perpendicularDistance(points.get(i), p1, p2);
            if (dist > maxDist) {
                maxDist = dist;
                maxIndex = i;
            }
        }

        if (maxDist > toleranceM) {
            douglasPeucker(points, start, maxIndex, toleranceM, keepIndices);
            douglasPeucker(points, maxIndex, end, toleranceM, keepIndices);
        }
    }

    private static double perpendicularDistance(BoundaryPointDto point,
                                                BoundaryPointDto lineStart,
                                                BoundaryPointDto lineEnd) {
        double lat = point.getLatitude();
        double lng = point.getLongitude();

        double latAvg = (lat + lineStart.getLatitude() + lineEnd.getLatitude()) / 3.0;
        double latRad = Math.toRadians(latAvg);
        double metersPerDegLat = 111132.0;
        double metersPerDegLng = 111320.0 * Math.cos(latRad);

        double x0 = (lng - lineStart.getLongitude()) * metersPerDegLng;
        double y0 = (lat - lineStart.getLatitude()) * metersPerDegLat;
        double x1 = (lineEnd.getLongitude() - lineStart.getLongitude()) * metersPerDegLng;
        double y1 = (lineEnd.getLatitude() - lineStart.getLatitude()) * metersPerDegLat;

        double numerator = Math.abs(y1 * x0 - x1 * y0);
        double denominator = Math.sqrt(x1 * x1 + y1 * y1);

        if (denominator == 0.0) {
            return haversineDistanceM(lat, lng, lineStart.getLatitude(), lineStart.getLongitude());
        }

        return numerator / denominator;
    }

    public static double haversineDistanceM(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}