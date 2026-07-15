-- V11__add_pto_audit_columns.sql
-- Add missing columns to pto_records table for full audit trail

-- Approval notes
ALTER TABLE pto_records ADD COLUMN IF NOT EXISTS approval_notes TEXT;

-- Suspension fields
ALTER TABLE pto_records ADD COLUMN IF NOT EXISTS suspended_by VARCHAR(50);
ALTER TABLE pto_records ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMP;
ALTER TABLE pto_records ADD COLUMN IF NOT EXISTS suspend_reason TEXT;

-- Reactivation fields
ALTER TABLE pto_records ADD COLUMN IF NOT EXISTS reactivated_by VARCHAR(50);
ALTER TABLE pto_records ADD COLUMN IF NOT EXISTS reactivated_at TIMESTAMP;
ALTER TABLE pto_records ADD COLUMN IF NOT EXISTS reactivate_notes TEXT;

-- Reinstatement fields
ALTER TABLE pto_records ADD COLUMN IF NOT EXISTS reinstated_by VARCHAR(50);
ALTER TABLE pto_records ADD COLUMN IF NOT EXISTS reinstated_at TIMESTAMP;
ALTER TABLE pto_records ADD COLUMN IF NOT EXISTS reinstate_reason TEXT;