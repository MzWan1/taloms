-- V29__drop_recursive_parcel_geometry_trigger.sql
-- Drop the infinite-recursion PostGIS trigger from V26
-- The trigger fired UPDATE parcels inside an AFTER UPDATE ON parcels, causing stack overflow

DROP TRIGGER IF EXISTS update_parcel_geometry ON parcels;
DROP FUNCTION IF EXISTS update_parcel_geometry();
