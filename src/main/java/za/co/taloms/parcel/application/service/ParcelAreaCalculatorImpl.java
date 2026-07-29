package za.co.taloms.parcel.application.service;

import org.springframework.stereotype.Service;
import za.co.taloms.parcel.application.dto.BoundaryPointDto;
import za.co.taloms.common.BusinessValidationException;
import java.util.List;

@Service
public class ParcelAreaCalculatorImpl implements ParcelAreaCalculator {

    private static final double R = 6378137.0;
    private static final double HAVERSINE_R = 6371000.0;
    private static final boolean IS_SOUTHERN_HEMISPHERE = true;

    @Override
    public Double calculateAreaM2(List<BoundaryPointDto> boundaries) {
        if (boundaries == null || boundaries.size() < 3) {
            throw new BusinessValidationException("Polygon must have at least 3 points");
        }

        int zone = determineUtmZone(boundaries);
        double[] origin = projectToUtm(boundaries.get(0).getLatitude(), boundaries.get(0).getLongitude(), zone);

        double sum1 = 0.0;
        double sum2 = 0.0;
        int n = boundaries.size();

        for (int i = 0; i < n; i++) {
            BoundaryPointDto p1 = boundaries.get(i);
            BoundaryPointDto p2 = boundaries.get((i + 1) % n);

            double[] utm1 = projectToUtm(p1.getLatitude(), p1.getLongitude(), zone);
            double[] utm2 = projectToUtm(p2.getLatitude(), p2.getLongitude(), zone);

            double x1 = utm1[0] - origin[0];
            double y1 = utm1[1] - origin[1];
            double x2 = utm2[0] - origin[0];
            double y2 = utm2[1] - origin[1];

            sum1 += x1 * y2;
            sum2 += x2 * y1;
        }

        double area = Math.abs(sum1 - sum2) / 2.0;
        return Math.round(area * 100.0) / 100.0;
    }

    @Override
    public Double calculatePerimeterM(List<BoundaryPointDto> boundaries) {
        if (boundaries == null || boundaries.size() < 2) {
            return 0.0;
        }

        double perimeter = 0.0;
        int n = boundaries.size();

        for (int i = 0; i < n; i++) {
            BoundaryPointDto p1 = boundaries.get(i);
            BoundaryPointDto p2 = boundaries.get((i + 1) % n);
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

        double latAvg = boundaries.stream()
                .mapToDouble(BoundaryPointDto::getLatitude)
                .average()
                .orElse(0.0);
        double lngAvg = boundaries.stream()
                .mapToDouble(BoundaryPointDto::getLongitude)
                .average()
                .orElse(0.0);

        double latRad = Math.toRadians(latAvg);
        double metersPerDegLat = 111132.0;
        double metersPerDegLng = 111320.0 * Math.cos(latRad);

        double cx = 0.0;
        double cy = 0.0;
        double signedArea = 0.0;
        int n = boundaries.size();

        for (int i = 0; i < n; i++) {
            BoundaryPointDto p1 = boundaries.get(i);
            BoundaryPointDto p2 = boundaries.get((i + 1) % n);

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
                    boundaries.stream().mapToDouble(BoundaryPointDto::getLatitude).average().orElse(0.0),
                    boundaries.stream().mapToDouble(BoundaryPointDto::getLongitude).average().orElse(0.0)
            };
        }

        cx /= (6.0 * signedArea);
        cy /= (6.0 * signedArea);

        double centroidLat = latAvg + (cy / metersPerDegLat);
        double centroidLng = lngAvg + (cx / metersPerDegLng);

        return new Double[]{centroidLat, centroidLng};
    }

    private static int determineUtmZone(List<BoundaryPointDto> boundaries) {
        double avgLng = boundaries.stream()
                .mapToDouble(BoundaryPointDto::getLongitude)
                .average()
                .orElse(28.0);
        int zone = (int) Math.floor((avgLng + 180.0) / 6.0) + 1;
        if (zone < 33) zone = 33;
        if (zone > 36) zone = 36;
        return zone;
    }

    private double[] projectToUtm(double lat, double lng, int zone) {
        double latRad = Math.toRadians(lat);
        double lngRad = Math.toRadians(lng);

        double centralMeridian = Math.toRadians(getCentralMeridian(zone));
        double falseEasting = 500000.0;
        double falseNorthing = IS_SOUTHERN_HEMISPHERE ? 10000000.0 : 0.0;
        double k0 = 0.9996;

        double n = R / Math.sqrt(1 - Math.pow(Math.sin(latRad), 2) * 0.00669438);
        double t = Math.tan(latRad) * Math.tan(latRad);
        double c = (0.00673839 / (1 - 0.00669438)) * Math.pow(Math.cos(latRad), 2);
        double a = Math.cos(latRad) * (lngRad - centralMeridian);

        double m = R * ((1 - 0.00669438 / 4 - 0.00669438 * 0.00669438 * 3 / 64)
                * latRad
                - (3 * 0.00669438 / 8 + 0.00669438 * 0.00669438 * 3 / 32)
                * Math.sin(2 * latRad)
                + (15 * 0.00669438 * 0.00669438 / 256)
                * Math.sin(4 * latRad));

        double easting = falseEasting + k0 * n * (a + (1 - t + c) * Math.pow(a, 3) / 6
                + (5 - 18 * t + Math.pow(t, 2) + 72 * c - 58 * 0.00669438) * Math.pow(a, 5) / 120);

        double northing = falseNorthing + k0 * (m + n * Math.tan(latRad) * (Math.pow(a, 2) / 2
                + (5 - t + 9 * c + 4 * Math.pow(c, 2)) * Math.pow(a, 4) / 24
                + (61 - 58 * t + Math.pow(t, 2) + 600 * c - 330 * 0.00669438) * Math.pow(a, 6) / 720));

        return new double[]{easting, northing};
    }

    private double[] unprojectFromUtm(double easting, double northing, int zone) {
        double k0 = 0.9996;
        double falseEasting = 500000.0;
        double falseNorthing = IS_SOUTHERN_HEMISPHERE ? 10000000.0 : 0.0;
        double centralMeridian = Math.toRadians(getCentralMeridian(zone));

        double e1 = Math.sqrt((1 - 0.00669438) / (1 + 0.00669438));
        double m = (northing - falseNorthing) / k0;
        double mu = m / (R * (1 - 0.00669438 / 4 - 0.00669438 * 0.00669438 * 3 / 64
                - 0.00669438 * 0.00669438 * 0.00669438 * 5 / 256));

        double e1Squared = e1 * e1;
        double j1 = (3 * e1 / 2 - 27 * Math.pow(e1, 3) / 32) * Math.sin(2 * mu);
        double j2 = (21 * e1 * e1 / 16 - 55 * Math.pow(e1, 4) / 32) * Math.sin(4 * mu);
        double j3 = (151 * Math.pow(e1, 3) / 96) * Math.sin(6 * mu);
        double j4 = (1097 * Math.pow(e1, 4) / 512) * Math.sin(8 * mu);

        double fp = mu + j1 + j2 + j3 + j4;

        double sinFp = Math.sin(fp);
        double cosFp = Math.cos(fp);
        double tanFp = Math.tan(fp);

        double n = R / Math.sqrt(1 - 0.00669438 * sinFp * sinFp);
        double rho = R * (1 - 0.00669438) / Math.pow(1 - 0.00669438 * sinFp * sinFp, 1.5);
        double psi = (1 - 0.00669438) / (1 - 0.00669438 * sinFp * sinFp);

        double ePrimeSquared = 0.00669438 * psi * psi;
        double e = (easting - falseEasting) / (n * k0);
        double eSquared = e * e;
        double eCubed = eSquared * e;

        double lat = fp - (n * tanFp / rho) * (eSquared / 2
                - (5 + 3 * psi + 10 * 0.00669438 - 4 * ePrimeSquared - 9 * 0.00669438) * eCubed * eSquared / 24
                + (61 + 90 * psi + 28 * 0.00669438 - 3 * ePrimeSquared) * Math.pow(e, 7) / 720);

        double lng = centralMeridian + (e / cosFp)
                - (1 + 2 * psi + 0.00669438) * e * eSquared / (6 * cosFp)
                + (5 - 2 * psi + 28 * 0.00669438 - 8 * ePrimeSquared + 24 * psi * psi) * Math.pow(e, 5) / (120 * cosFp)
                + (61 - 479 * psi + 179 * 0.00669438 - psi * psi) * Math.pow(e, 7) / (5040 * cosFp);

        return new double[]{Math.toDegrees(lat), Math.toDegrees(lng)};
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

    private static int getCentralMeridian(int zone) {
        return -177 + 6 * (zone - 1);
    }
}
