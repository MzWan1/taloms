-- V39__add_business_id_to_residents.sql
-- Link residents to businesses for cross-module synchronization

ALTER TABLE residents ADD COLUMN IF NOT EXISTS business_id BIGINT;

COMMENT ON COLUMN residents.business_id IS 'References the business this resident is associated with (e.g. owner/employee)';
