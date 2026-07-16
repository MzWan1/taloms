-- V14__create_resident_tables.sql
-- Resident table for managing individuals within households

-- Residents table
CREATE TABLE IF NOT EXISTS residents (
                                         id BIGSERIAL PRIMARY KEY,
                                         full_name VARCHAR(150) NOT NULL,
    id_number VARCHAR(13) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) NOT NULL,
    relationship_type VARCHAR(20) NOT NULL,
    occupation VARCHAR(100),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(150),
    household_id BIGINT NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    notes TEXT,
    created_by VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_resident_household FOREIGN KEY (household_id) REFERENCES households(id),
    CONSTRAINT uq_resident_id_number UNIQUE (id_number)
    );

-- Create indexes for faster queries
CREATE INDEX idx_residents_household_id ON residents(household_id);
CREATE INDEX idx_residents_id_number ON residents(id_number);
CREATE INDEX idx_residents_full_name ON residents(full_name);
CREATE INDEX idx_residents_active ON residents(active);
CREATE INDEX idx_residents_relationship_type ON residents(relationship_type);

-- Create trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION update_resident_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_residents_updated_at
    BEFORE UPDATE ON residents
    FOR EACH ROW
    EXECUTE FUNCTION update_resident_updated_at();

-- Comments for documentation
COMMENT ON TABLE residents IS 'Individual residents belonging to households';
COMMENT ON COLUMN residents.full_name IS 'Full name of the resident';
COMMENT ON COLUMN residents.id_number IS 'SA ID number (unique across system)';
COMMENT ON COLUMN residents.gender IS 'MALE, FEMALE, OTHER';
COMMENT ON COLUMN residents.relationship_type IS 'HOUSEHOLD_HEAD, SPOUSE, CHILD, PARENT, SIBLING, GRANDPARENT, GRANDCHILD, OTHER';
COMMENT ON COLUMN residents.household_id IS 'The household this resident belongs to';