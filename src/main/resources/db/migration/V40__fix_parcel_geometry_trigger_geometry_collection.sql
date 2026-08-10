-- V40__fix_parcel_geometry_trigger_geometry_collection.sql
-- Fix: handle ST_MakeValid returning GeometryCollection
-- Previously, if ST_MakeValid returned a GeometryCollection (e.g. for self-intersecting
-- polygons), the UPDATE would fail with:
--   "Geometry type (GeometryCollection) does not match column type (Polygon)"
-- This rolled back the entire transaction, so boundary coordinates were never persisted.

DROP TRIGGER IF EXISTS update_parcel_geometry_boundaries ON parcel_boundaries;
DROP FUNCTION IF EXISTS update_parcel_geometry_from_boundaries();

CREATE OR REPLACE FUNCTION update_parcel_geometry_from_boundaries()
RETURNS TRIGGER AS $$
DECLARE
    ring_coords TEXT;
    parcel_id_val BIGINT;
    point_count BIGINT;
    geom geometry;
BEGIN
    parcel_id_val := COALESCE(NEW.parcel_id, OLD.parcel_id);

    SELECT COUNT(*) INTO point_count
    FROM parcel_boundaries
    WHERE parcel_id = parcel_id_val;

    IF point_count < 3 THEN
        RETURN COALESCE(NEW, OLD);
    END IF;

    SELECT STRING_AGG(longitude || ' ' || latitude, ', ' ORDER BY sequence) INTO ring_coords
    FROM parcel_boundaries
    WHERE parcel_id = parcel_id_val;

    IF ring_coords IS NOT NULL AND LENGTH(ring_coords) > 0 THEN
        ring_coords := ring_coords || ', ' || SPLIT_PART(ring_coords, ', ', 1);

        geom := ST_MakeValid(ST_GeomFromText('POLYGON((' || ring_coords || '))', 4326));

        IF ST_GeometryType(geom) = 'ST_MultiPolygon' THEN
            geom := ST_GeometryN(geom, 1);
        ELSIF ST_GeometryType(geom) = 'ST_GeometryCollection' THEN
            geom := ST_CollectionExtract(geom, 3);
            IF ST_GeometryType(geom) = 'ST_MultiPolygon' THEN
                geom := ST_GeometryN(geom, 1);
            END IF;
        END IF;

        IF geom IS NOT NULL AND ST_GeometryType(geom) = 'ST_Polygon' THEN
            UPDATE parcels
            SET geometry = ST_SetSRID(geom, 4326)
            WHERE id = parcel_id_val;
        END IF;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_parcel_geometry_boundaries
    AFTER INSERT OR UPDATE OR DELETE ON parcel_boundaries
    FOR EACH ROW
    EXECUTE FUNCTION update_parcel_geometry_from_boundaries();
