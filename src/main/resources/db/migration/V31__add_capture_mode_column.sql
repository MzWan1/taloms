-- V31__add_capture_mode_column.sql
-- Add capture_mode column to parcels table to record the boundary capture method

ALTER TABLE parcels ADD COLUMN IF NOT EXISTS capture_mode VARCHAR(30) NOT NULL DEFAULT 'MANUAL_TAP';

COMMENT ON COLUMN parcels.capture_mode IS 'MANUAL_TAP, AUTO_WALK, HYBRID, DRONE_ASSIST';
