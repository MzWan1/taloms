-- V28__add_pto_soft_delete_columns.sql
-- Add soft-delete columns to pto_records table
-- Enables audit trail and historical reporting for deleted PTO entries

ALTER TABLE pto_records
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_pto_deleted_at ON pto_records(deleted_at);
CREATE INDEX IF NOT EXISTS idx_pto_deleted_by ON pto_records(deleted_by);
