-- V12__create_parcel_tables.sql
-- Parcel and Parcel Boundary tables for land parcel management

-- Enable PostGIS extension if not already enabled
CREATE EXTENSION IF NOT EXISTS postgis;

-- Parcels table
CREATE TABLE IF NOT EXISTS parcels (
                                       id BIGSERIAL PRIMARY KEY,
                                       parcel_number VARCHAR(20) NOT NULL UNIQUE,
    stand_number VARCHAR(20) NOT NULL,
    parcel_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    area_m2 DOUBLE PRECISION,
    area_hectares DOUBLE PRECISION,
    centroid_lat DOUBLE PRECISION,
    centroid_lng DOUBLE PRECISION,
    village_id BIGINT NOT NULL,
    pto_id BIGINT,
    notes TEXT,
    created_by VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_parcel_village FOREIGN KEY (village_id) REFERENCES villages(id),
    CONSTRAINT fk_parcel_pto FOREIGN KEY (pto_id) REFERENCES pto_records(id),
    CONSTRAINT uq_stand_number_village UNIQUE (stand_number, village_id)
    );

-- Create index for faster queries
CREATE INDEX idx_parcels_village_id ON parcels(village_id);
CREATE INDEX idx_parcels_status ON parcels(status);
CREATE INDEX idx_parcels_parcel_number ON parcels(parcel_number);
CREATE INDEX idx_parcels_stand_number ON parcels(stand_number);

-- Parcel boundaries table (stores polygon vertices in order)
CREATE TABLE IF NOT EXISTS parcel_boundaries (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 parcel_id BIGINT NOT NULL,
                                                 sequence INTEGER NOT NULL,
                                                 latitude DOUBLE PRECISION NOT NULL,
                                                 longitude DOUBLE PRECISION NOT NULL,
                                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                 CONSTRAINT fk_boundary_parcel FOREIGN KEY (parcel_id) REFERENCES parcels(id) ON DELETE CASCADE,
    CONSTRAINT uq_boundary_parcel_sequence UNIQUE (parcel_id, sequence)
    );

-- Create index for boundary queries
CREATE INDEX idx_boundaries_parcel_id ON parcel_boundaries(parcel_id);

-- Add geometry column for PostGIS spatial queries (after table creation)
SELECT AddGeometryColumn('public', 'parcels', 'geometry', 4326, 'POLYGON', 2);

-- Create spatial index for fast overlap detection
CREATE INDEX idx_parcels_geometry ON parcels USING GIST (geometry);

-- Create trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION update_parcel_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_parcels_updated_at
    BEFORE UPDATE ON parcels
    FOR EACH ROW
    EXECUTE FUNCTION update_parcel_updated_at();

-- Comments for documentation
COMMENT ON TABLE parcels IS 'Land parcels/stands registered under Traditional Authorities';
COMMENT ON COLUMN parcels.parcel_number IS 'Unique system-generated parcel identifier (PRC-YYYY-NNNNN)';
COMMENT ON COLUMN parcels.stand_number IS 'Local stand number unique within village';
COMMENT ON COLUMN parcels.parcel_type IS 'RESIDENTIAL, BUSINESS, AGRICULTURAL, COMMUNAL, RESERVED';
COMMENT ON COLUMN parcels.status IS 'AVAILABLE, ALLOCATED, DISPUTED, RESERVED, INACTIVE';
COMMENT ON COLUMN parcels.geometry IS 'PostGIS polygon geometry for the parcel boundary';