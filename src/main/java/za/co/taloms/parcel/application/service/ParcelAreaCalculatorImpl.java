package za.co.taloms.parcel.application.service;

import org.springframework.stereotype.Service;
import za.co.taloms.parcel.application.dto.BoundaryPointDto;
import za.co.taloms.common.BusinessValidationException;
import java.util.List;

/**
 * Calculates parcel area and centroid using UTM Zone 35S projection for
 * accurate measurements in South Africa.
 *
 * UTM Zone 35S covers most of Limpopo, Mpumalanga, and KwaZulu-Natal
 * where the majority of traditional authorities are located.
 */
@Service
public class ParcelAreaCalculatorImpl implements ParcelAreaCalculator {

    private static final double R = 6378137.0;
    private static final double HAVERSINE_R = 6371000.0;
    private static final double LAT_ORIGIN = 0.0;
    private static final int UTM_ZONE = 35;
    private static final boolean IS_SOUTHERN_HEMISPHERE = true;

    @Override
    public Double calculateAreaM2(List<BoundaryPointDto> boundaries) {
        if (boundaries == null || boundaries.size() < 3) {
            throw new BusinessValidationException("Polygon must have at least 3 points");
        }

        double[] origin = projectToUtm(boundaries.get(0).getLatitude(), boundaries.get(0).getLongitude());

        double sum1 = 0.0;
        double sum2 = 0.0;
        int n = boundaries.size();

        for (int i = 0; i < n; i++) {
            BoundaryPointDto p1 = boundaries.get(i);
            BoundaryPointDto p2 = boundaries.get((i + 1) % n);

            double[] utm1 = projectToUtm(p1.getLatitude(), p1.getLongitude());
            double[] utm2 = projectToUtm(p2.getLatitude(), p2.getLongitude());

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

        double area = calculateAreaM2(boundaries);
        if (area == 0.0) {
            return new Double[]{
                    boundaries.stream().mapToDouble(BoundaryPointDto::getLatitude).average().orElse(0.0),
                    boundaries.stream().mapToDouble(BoundaryPointDto::getLongitude).average().orElse(0.0)
            };
        }

        double[] origin = projectToUtm(boundaries.get(0).getLatitude(), boundaries.get(0).getLongitude());

        double cx = 0.0;
        double cy = 0.0;
        int n = boundaries.size();

        for (int i = 0; i < n; i++) {
            BoundaryPointDto p1 = boundaries.get(i);
            BoundaryPointDto p2 = boundaries.get((i + 1) % n);

            double[] utm1 = projectToUtm(p1.getLatitude(), p1.getLongitude());
            double[] utm2 = projectToUtm(p2.getLatitude(), p2.getLongitude());

            double x1 = utm1[0] - origin[0];
            double y1 = utm1[1] - origin[1];
            double x2 = utm2[0] - origin[0];
            double y2 = utm2[1] - origin[1];

            double cross = x1 * y2 - x2 * y1;
            cx += (x1 + x2) * cross;
            cy += (y1 + y2) * cross;
        }

        cx /= (6.0 * area);
        cy /= (6.0 * area);

        double[] centroidUtm = {cx + origin[0], cy + origin[1]};
        double[] centroidWgs = unprojectFromUtm(centroidUtm[0], centroidUtm[1]);

        return new Double[]{centroidWgs[0], centroidWgs[1]};
    }

    /**
     * Project WGS84 lat/lng to UTM Zone 35S metres.
     * Returns [easting, northing] in metres.
     */
    private double[] projectToUtm(double lat, double lng) {
        double latRad = Math.toRadians(lat);
        double lngRad = Math.toRadians(lng);

        double centralMeridian = Math.toRadians(getCentralMeridian(UTM_ZONE));
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

    /**
     * Convert UTM Zone 35S metres back to WGS84 lat/lng.
     */
    private double[] unprojectFromUtm(double easting, double northing) {
        double k0 = 0.9996;
        double falseEasting = 500000.0;
        double falseNorthing = IS_SOUTHERN_HEMISPHERE ? 10000000.0 : 0.0;
        double centralMeridian = Math.toRadians(getCentralMeridian(UTM_ZONE));

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

    private static double getCentralMeridian(int zone) {
        return -177 + 6 * zone;
    }
}


