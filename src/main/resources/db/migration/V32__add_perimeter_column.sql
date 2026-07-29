-- V32__add_perimeter_column.sql
-- Add perimeter_m column to parcels table for storing calculated perimeter

ALTER TABLE parcels ADD COLUMN IF NOT EXISTS perimeter_m DOUBLE PRECISION;
