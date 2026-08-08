-- V38__add_household_id_to_business_occupancies.sql
-- Link business occupancies to households for module synchronization

ALTER TABLE business_occupancies ADD COLUMN IF NOT EXISTS household_id BIGINT;

COMMENT ON COLUMN business_occupancies.household_id IS 'References the household that operates this business (derived from parcel during PTO approval)';
