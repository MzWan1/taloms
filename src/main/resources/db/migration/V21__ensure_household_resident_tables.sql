-- V21: Ensure household and resident tables exist (idempotent)
-- This guards against deployments where V13/V14 never executed (e.g. a pre-existing
-- database that was baselined). Every statement is safe to re-run.

-- ============================================================
-- HOUSEHOLDS
-- ============================================================
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

-- Partial unique index to enforce only one active household per parcel (PostgreSQL)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'idx_households_unique_active_parcel'
    ) THEN
        EXECUTE 'CREATE UNIQUE INDEX idx_households_unique_active_parcel
                     ON households (parcel_id)
                     WHERE active = true';
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_households_parcel_id ON households(parcel_id);
CREATE INDEX IF NOT EXISTS idx_households_pto_id ON households(pto_id);
CREATE INDEX IF NOT EXISTS idx_households_household_head_name ON households(household_head_name);
CREATE INDEX IF NOT EXISTS idx_households_active ON households(active);

-- Auto-update updated_at trigger
CREATE OR REPLACE FUNCTION update_household_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_households_updated_at ON households;
CREATE TRIGGER update_households_updated_at
    BEFORE UPDATE ON households
    FOR EACH ROW
    EXECUTE FUNCTION update_household_updated_at();

-- ============================================================
-- RESIDENTS
-- ============================================================
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

CREATE INDEX IF NOT EXISTS idx_residents_household_id ON residents(household_id);
CREATE INDEX IF NOT EXISTS idx_residents_id_number ON residents(id_number);
CREATE INDEX IF NOT EXISTS idx_residents_full_name ON residents(full_name);
CREATE INDEX IF NOT EXISTS idx_residents_active ON residents(active);
CREATE INDEX IF NOT EXISTS idx_residents_relationship_type ON residents(relationship_type);

CREATE OR REPLACE FUNCTION update_resident_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_residents_updated_at ON residents;
CREATE TRIGGER update_residents_updated_at
    BEFORE UPDATE ON residents
    FOR EACH ROW
    EXECUTE FUNCTION update_resident_updated_at();
