-- V55__add_parcel_overlap_trigger.sql
-- Enforces that no two active parcels overlap in area, while allowing them to touch boundaries.

CREATE OR REPLACE FUNCTION check_parcel_overlap()
RETURNS TRIGGER AS $$
DECLARE
    overlapping_parcel_number VARCHAR;
BEGIN
    IF NEW.geometry IS NOT NULL AND NEW.status != 'INACTIVE' THEN
        SELECT parcel_number INTO overlapping_parcel_number
        FROM parcels
        WHERE id != NEW.id
          AND status != 'INACTIVE'
          AND geometry IS NOT NULL
          AND ST_Intersects(geometry, ST_MakeValid(NEW.geometry))
          AND NOT ST_Touches(geometry, ST_MakeValid(NEW.geometry))
        LIMIT 1;

        IF overlapping_parcel_number IS NOT NULL THEN
            RAISE EXCEPTION 'Parcel geometry overlaps with existing active parcel: %', overlapping_parcel_number;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER enforce_no_parcel_overlap
    BEFORE INSERT OR UPDATE OF geometry, status ON parcels
    FOR EACH ROW
    EXECUTE FUNCTION check_parcel_overlap();

