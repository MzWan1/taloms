-- V37__add_chief_headman_to_parcels.sql
-- Inherit leadership data from Village/Traditional Authority onto Parcel
-- so that downstream records (PTO) can reference parcel-level authority info

ALTER TABLE parcels ADD COLUMN IF NOT EXISTS chief_name VARCHAR(150);
ALTER TABLE parcels ADD COLUMN IF NOT EXISTS headman_name VARCHAR(150);

COMMENT ON COLUMN parcels.chief_name IS 'Chief name inherited from the parcel''s village traditional authority at creation time';
COMMENT ON COLUMN parcels.headman_name IS 'Headman name inherited from the parcel''s village at creation time';
