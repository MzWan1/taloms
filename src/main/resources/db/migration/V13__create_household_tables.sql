-- V13__create_household_tables.sql
-- Household table for managing households linked to parcels and PTOs

-- Households table
CREATE TABLE IF NOT EXISTS households (
                                          id BIGSERIAL PRIMARY KEY,
                                          household_head_name VARCHAR(150) NOT NULL,
    household_head_id_number VARCHAR(13) NOT NULL,
    contact_phone VARCHAR(20),
    contact_email VARCHAR(150),
    parcel_id BIGINT NOT NULL,
    pto_id BIGINT,
    registration_date DATE NOT NULL DEFAULT CURRENT_DATE,
    active BOOLEAN DEFAULT TRUE,
    notes TEXT,
    created_by VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_household_parcel FOREIGN KEY (parcel_id) REFERENCES parcels(id),
    CONSTRAINT fk_household_pto FOREIGN KEY (pto_id) REFERENCES pto_records(id)
    );

-- Create a partial unique index to enforce only one active household per parcel
-- This is the PostgreSQL way to enforce "WHERE active = true"
CREATE UNIQUE INDEX idx_households_unique_active_parcel
    ON households (parcel_id)
    WHERE active = true;

-- Create indexes for faster queries
CREATE INDEX idx_households_parcel_id ON households(parcel_id);
CREATE INDEX idx_households_pto_id ON households(pto_id);
CREATE INDEX idx_households_household_head_name ON households(household_head_name);
CREATE INDEX idx_households_active ON households(active);

-- Create trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION update_household_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_households_updated_at
    BEFORE UPDATE ON households
    FOR EACH ROW
    EXECUTE FUNCTION update_household_updated_at();

-- Comments for documentation
COMMENT ON TABLE households IS 'Household records linked to parcels and PTOs';
COMMENT ON COLUMN households.household_head_name IS 'Full name of the household head';
COMMENT ON COLUMN households.household_head_id_number IS 'SA ID number of the household head';
COMMENT ON COLUMN households.parcel_id IS 'The parcel this household resides on';
COMMENT ON COLUMN households.pto_id IS 'The PTO associated with this household';
COMMENT ON COLUMN households.active IS 'Whether this is the active household on the parcel (only one active per parcel)';