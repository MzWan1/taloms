-- V33__add_parcel_type_column.sql
-- Re-add parcel_type column to parcels table for parcel classification

ALTER TABLE parcels ADD COLUMN IF NOT EXISTS parcel_type VARCHAR(30) NOT NULL DEFAULT 'RESIDENTIAL';

-- Update existing parcels to have a default parcel_type
UPDATE parcels SET parcel_type = 'RESIDENTIAL' WHERE parcel_type IS NULL;