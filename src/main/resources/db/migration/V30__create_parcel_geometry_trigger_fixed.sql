-- V30__create_parcel_geometry_trigger_fixed.sql
-- Auto-update PostGIS geometry column when parcel_boundaries are saved
-- This trigger fires on parcel_boundaries changes to avoid infinite recursion
-- (previous V26 trigger fired on parcels AFTER UPDATE and caused stack overflow)

-- Drop existing trigger if it exists (idempotent)
DROP TRIGGER IF EXISTS update_parcel_geometry_boundaries ON parcel_boundaries;
DROP FUNCTION IF EXISTS update_parcel_geometry_from_boundaries();

-- Create function to rebuild polygon from boundary points
CREATE OR REPLACE FUNCTION update_parcel_geometry_from_boundaries()
RETURNS TRIGGER AS $$
DECLARE
    ring_coords TEXT;
    parcel_id_val BIGINT;
BEGIN
    parcel_id_val := COALESCE(NEW.parcel_id, OLD.parcel_id);

    -- Rebuild WKT polygon from parcel_boundaries ordered by sequence
    SELECT STRING_AGG(longitude || ' ' || latitude, ', ' ORDER BY sequence) INTO ring_coords
    FROM parcel_boundaries
    WHERE parcel_id = parcel_id_val;

    IF ring_coords IS NOT NULL AND LENGTH(ring_coords) > 0 THEN
        -- Close the ring by appending the first point
        ring_coords := ring_coords || ', ' || SPLIT_PART(ring_coords, ', ', 1);

        -- Use ST_MakeValid to auto-repair self-intersections
        UPDATE parcels
        SET geometry = ST_SetSRID(ST_MakeValid(ST_GeomFromText('POLYGON((' || ring_coords || '))', 4326)), 4326)
        WHERE id = parcel_id_val;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Trigger fired after parcel_boundaries are inserted, updated, or deleted
CREATE TRIGGER update_parcel_geometry_boundaries
    AFTER INSERT OR UPDATE OR DELETE ON parcel_boundaries
    FOR EACH ROW
    EXECUTE FUNCTION update_parcel_geometry_from_boundaries();
