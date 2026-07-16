-- V15__create_business_occupancy_tables.sql
-- Business Occupancy table for managing commercial stands

-- Business Occupancies table
CREATE TABLE IF NOT EXISTS business_occupancies (
                                                    id BIGSERIAL PRIMARY KEY,
                                                    business_name VARCHAR(200) NOT NULL,
    registration_number VARCHAR(50),
    business_type VARCHAR(50) NOT NULL,
    owner_name VARCHAR(150) NOT NULL,
    owner_id_number VARCHAR(13) NOT NULL,
    contact_phone VARCHAR(20),
    contact_email VARCHAR(150),
    parcel_id BIGINT NOT NULL,
    pto_id BIGINT NOT NULL,
    operating_hours VARCHAR(200),
    employees_count INTEGER DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    notes TEXT,
    created_by VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_business_parcel FOREIGN KEY (parcel_id) REFERENCES parcels(id),
    CONSTRAINT fk_business_pto FOREIGN KEY (pto_id) REFERENCES pto_records(id),
    CONSTRAINT uq_business_parcel UNIQUE (parcel_id)
    );

-- Create indexes for faster queries
CREATE INDEX idx_business_parcel_id ON business_occupancies(parcel_id);
CREATE INDEX idx_business_pto_id ON business_occupancies(pto_id);
CREATE INDEX idx_business_business_name ON business_occupancies(business_name);
CREATE INDEX idx_business_owner_name ON business_occupancies(owner_name);
CREATE INDEX idx_business_status ON business_occupancies(status);
CREATE INDEX idx_business_registration_number ON business_occupancies(registration_number);

-- Create trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION update_business_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_business_updated_at
    BEFORE UPDATE ON business_occupancies
    FOR EACH ROW
    EXECUTE FUNCTION update_business_updated_at();

-- Comments for documentation
COMMENT ON TABLE business_occupancies IS 'Business/commercial occupancy records on Traditional Authority land';
COMMENT ON COLUMN business_occupancies.business_name IS 'Registered business name';
COMMENT ON COLUMN business_occupancies.registration_number IS 'Company/CC registration number (optional)';
COMMENT ON COLUMN business_occupancies.business_type IS 'RETAIL, WHOLESALE, RESTAURANT, OFFICE, WAREHOUSE, OTHER';
COMMENT ON COLUMN business_occupancies.status IS 'ACTIVE, INACTIVE, PENDING, SUSPENDED';
COMMENT ON COLUMN business_occupancies.parcel_id IS 'The parcel where the business operates';
COMMENT ON COLUMN business_occupancies.pto_id IS 'The PTO associated with this business';