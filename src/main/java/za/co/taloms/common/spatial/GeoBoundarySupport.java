package za.co.taloms.common.spatial;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

import java.util.List;

/**
 * Core polygon-geometry helpers shared by authority/village/parcel boundary
 * validation. Coordinates are supplied as {@code double[]{lat, lng}} and are
 * converted to JTS polygons in (x=lng, y=lat) order.
 */
public final class GeoBoundarySupport {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private GeoBoundarySupport() {
    }

    /** Builds a closed JTS polygon from an open list of [lat, lng] points. */
    public static Polygon toPolygon(List<double[]> latLngPoints) {
        if (latLngPoints == null || latLngPoints.size() < 3) {
            return null;
        }
        Coordinate[] shell = new Coordinate[latLngPoints.size() + 1];
        for (int i = 0; i < latLngPoints.size(); i++) {
            // JTS order: x = longitude, y = latitude
            shell[i] = new Coordinate(latLngPoints.get(i)[1], latLngPoints.get(i)[0]);
        }
        shell[latLngPoints.size()] = shell[0]; // close the ring
        LinearRing ring = GF.createLinearRing(shell);
        return GF.createPolygon(ring, null);
    }

    /** True when {@code inner} lies fully within {@code outer}. */
    public static boolean covers(Geometry outer, Geometry inner) {
        if (outer == null || inner == null) {
            return false;
        }
        PreparedGeometry prepared = PreparedGeometryFactory.prepare(outer);
        return prepared.covers(inner);
    }

    /** True when the two polygons share any area. */
    public static boolean overlaps(Geometry a, Geometry b) {
        if (a == null || b == null) {
            return false;
        }
        PreparedGeometry prepared = PreparedGeometryFactory.prepare(a);
        return prepared.intersects(b);
    }

    /** Quick bbox rejection helper. */
    public static boolean envelopesIntersect(Geometry a, Geometry b) {
        if (a == null || b == null) {
            return false;
        }
        Envelope ea = a.getEnvelopeInternal();
        Envelope eb = b.getEnvelopeInternal();
        return ea.intersects(eb);
    }
}
