package za.co.taloms.parcel.application.service;

import za.co.taloms.parcel.application.dto.BoundaryPointDto;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for simplifying GPS boundary traces using the Douglas-Peucker algorithm.
 *
 * Reduces hundreds of raw walk-trace points to 20-40 meaningful vertices
 * while preserving the true shape of the parcel boundary.
 */
public class BoundarySimplifier {

    /**
     * Simplify a list of boundary points using Douglas-Peucker.
     *
     * @param points     the original boundary points (must be ordered)
     * @param toleranceM tolerance in metres — points closer than this to the line
     *                   between start and end are discarded
     * @return simplified list of boundary points
     */
    public static List<BoundaryPointDto> simplify(List<BoundaryPointDto> points, double toleranceM) {
        if (points == null || points.size() < 3) {
            return points;
        }

        List<Integer> keepIndices = new ArrayList<>();
        douglasPeucker(points, 0, points.size() - 1, toleranceM, keepIndices);

        keepIndices.sort(Integer::compareTo);

        List<BoundaryPointDto> result = new ArrayList<>();
        for (int idx : keepIndices) {
            result.add(points.get(idx));
        }

        return result;
    }

    private static void douglasPeucker(List<BoundaryPointDto> points,
                                       int start, int end,
                                       double toleranceM,
                                       List<Integer> keepIndices) {
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

        double x0 = lng;
        double y0 = lat;
        double x1 = lineStart.getLongitude();
        double y1 = lineStart.getLatitude();
        double x2 = lineEnd.getLongitude();
        double y2 = lineEnd.getLatitude();

        double numerator = Math.abs(
                (y2 - y1) * x0 - (x2 - x1) * y0 + x2 * y1 - y2 * x1
        );
        double denominator = Math.sqrt(
                Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2)
        );

        if (denominator == 0.0) {
            return haversineDistanceM(lat, lng, lineStart.getLatitude(), lineStart.getLongitude());
        }

        double perpendicularDegrees = numerator / denominator;
        return perpendicularDegrees * 111320.0;
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
