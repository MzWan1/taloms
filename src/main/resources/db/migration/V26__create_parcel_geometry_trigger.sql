-- V26__create_parcel_geometry_trigger.sql
-- Auto-update PostGIS geometry column when parcel boundaries are saved

-- Drop existing trigger if it exists (idempotent)
DROP TRIGGER IF EXISTS update_parcel_geometry ON parcels;
DROP FUNCTION IF EXISTS update_parcel_geometry();

-- Create function to rebuild polygon from boundary points
CREATE OR REPLACE FUNCTION update_parcel_geometry()
RETURNS TRIGGER AS $$
DECLARE
    ring_coords TEXT;
BEGIN
    -- Rebuild WKT polygon from parcel_boundaries ordered by sequence
    SELECT STRING_AGG(longitude || ' ' || latitude, ', ' ORDER BY sequence) INTO ring_coords
    FROM parcel_boundaries
    WHERE parcel_id = NEW.id;

    IF ring_coords IS NOT NULL AND LENGTH(ring_coords) > 0 THEN
        -- Close the ring by appending the first point
        ring_coords := ring_coords || ', ' || SPLIT_PART(ring_coords, ', ', 1);
        UPDATE parcels
        SET geometry = ST_GeomFromText('POLYGON((' || ring_coords || '))', 4326)
        WHERE id = NEW.id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger fired after parcel is saved (create or update)
CREATE TRIGGER update_parcel_geometry
    AFTER INSERT OR UPDATE ON parcels
    FOR EACH ROW
    EXECUTE FUNCTION update_parcel_geometry();
