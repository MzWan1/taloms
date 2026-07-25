-- V24__extend_pto_with_allocation_and_survey_fields.sql
-- Add allocation, survey reference, and digital workflow fields to pto_records
-- aligned with legal requirements for Traditional Authority PTO issuance

ALTER TABLE pto_records
    ADD COLUMN IF NOT EXISTS allocated_by VARCHAR(150),
    ADD COLUMN IF NOT EXISTS allocation_date DATE,
    ADD COLUMN IF NOT EXISTS stand_area DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS survey_reference VARCHAR(100),
    ADD COLUMN IF NOT EXISTS boundary_description TEXT,
    ADD COLUMN IF NOT EXISTS allocation_fee_receipt VARCHAR(100),
    ADD COLUMN IF NOT EXISTS ta_recommendation_ref VARCHAR(100),
    ADD COLUMN IF NOT EXISTS community_resolution_required BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_pto_allocated_by ON pto_records(allocated_by);
CREATE INDEX IF NOT EXISTS idx_pto_survey_reference ON pto_records(survey_reference);
CREATE INDEX IF NOT EXISTS idx_pto_allocation_date ON pto_records(allocation_date);
