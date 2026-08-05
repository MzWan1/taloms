-- V34__fix_parcel_geometry_trigger_min_points.sql
-- Fix: skip geometry update when fewer than 3 boundary points exist
-- The trigger was firing after each row insert during saveAll(), and
-- PostGIS requires at least 3 distinct points to form a valid polygon.

DROP TRIGGER IF EXISTS update_parcel_geometry_boundaries ON parcel_boundaries;
DROP FUNCTION IF EXISTS update_parcel_geometry_from_boundaries();

CREATE OR REPLACE FUNCTION update_parcel_geometry_from_boundaries()
RETURNS TRIGGER AS $$
DECLARE
    ring_coords TEXT;
    parcel_id_val BIGINT;
    point_count BIGINT;
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

        UPDATE parcels
        SET geometry = ST_SetSRID(ST_MakeValid(ST_GeomFromText('POLYGON((' || ring_coords || '))', 4326)), 4326)
        WHERE id = parcel_id_val;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_parcel_geometry_boundaries
    AFTER INSERT OR UPDATE OR DELETE ON parcel_boundaries
    FOR EACH ROW
    EXECUTE FUNCTION update_parcel_geometry_from_boundaries();