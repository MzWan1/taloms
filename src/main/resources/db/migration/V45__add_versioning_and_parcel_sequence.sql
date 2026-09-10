-- V45__add_versioning_and_parcel_sequence.sql
-- Optimistic locking version columns for offline-first sync
-- and a database sequence for concurrency-safe parcel numbers

-- parcels version column
ALTER TABLE parcels ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- parcel_boundaries version column
ALTER TABLE parcel_boundaries ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- index for version lookups during sync
CREATE INDEX IF NOT EXISTS idx_parcels_version ON parcels(version);
CREATE INDEX IF NOT EXISTS idx_boundaries_version ON parcel_boundaries(version);

-- sequence for concurrency-safe parcel number generation
CREATE SEQUENCE IF NOT EXISTS parcel_number_seq START 1;
