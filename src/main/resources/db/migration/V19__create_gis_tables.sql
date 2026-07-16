-- V19__create_gis_tables.sql
-- GIS Layer configuration for map overlays

CREATE TABLE IF NOT EXISTS gis_layers (
                                          id BIGSERIAL PRIMARY KEY,
                                          name VARCHAR(100) NOT NULL,
    description TEXT,
    layer_type VARCHAR(30) NOT NULL, -- VECTOR, RASTER, TILE
    source_url VARCHAR(500),
    visible BOOLEAN DEFAULT TRUE,
    opacity DECIMAL(3,2) DEFAULT 1.0,
    z_index INTEGER DEFAULT 0,
    created_by VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- Create indexes
CREATE INDEX idx_gis_layers_visible ON gis_layers(visible);

-- Comments
COMMENT ON TABLE gis_layers IS 'GIS layer configurations for map display';
COMMENT ON COLUMN gis_layers.layer_type IS 'VECTOR, RASTER, TILE';