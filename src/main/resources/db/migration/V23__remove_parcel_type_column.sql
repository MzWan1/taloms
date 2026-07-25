-- V23__remove_parcel_type_column.sql
-- Remove parcel_type column from parcels table since parcel type is now determined by PTO purpose

ALTER TABLE parcels DROP COLUMN IF EXISTS parcel_type;
