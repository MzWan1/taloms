-- V18__create_audit_tables.sql
-- Audit Trail tables for tracking all state-changing actions

-- Audit Log table
CREATE TABLE IF NOT EXISTS audit_logs (
                                          id BIGSERIAL PRIMARY KEY,
                                          entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    previous_value JSONB,
    new_value JSONB,
    performed_by VARCHAR(50) NOT NULL,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    description TEXT
    );

-- Create indexes for faster queries
CREATE INDEX idx_audit_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_entity_id ON audit_logs(entity_id);
CREATE INDEX idx_audit_performed_by ON audit_logs(performed_by);
CREATE INDEX idx_audit_performed_at ON audit_logs(performed_at);
CREATE INDEX idx_audit_action ON audit_logs(action);

-- Composite indexes for common queries
CREATE INDEX idx_audit_entity_lookup ON audit_logs(entity_type, entity_id, performed_at DESC);
CREATE INDEX idx_audit_user_lookup ON audit_logs(performed_by, performed_at DESC);

-- Comments for documentation
COMMENT ON TABLE audit_logs IS 'Immutable audit trail for all state-changing actions';
COMMENT ON COLUMN audit_logs.entity_type IS 'PTO, PARCEL, RESIDENT, HOUSEHOLD, BUSINESS, USER, AUTHORITY, VILLAGE, DOCUMENT';
COMMENT ON COLUMN audit_logs.action IS 'CREATE, UPDATE, DELETE, APPROVE, REVOKE, SUSPEND, REACTIVATE, REINSTATE, ACTIVATE, DEACTIVATE';
COMMENT ON COLUMN audit_logs.previous_value IS 'JSON representation of the entity before the change';
COMMENT ON COLUMN audit_logs.new_value IS 'JSON representation of the entity after the change';
COMMENT ON COLUMN audit_logs.performed_by IS 'Username of the user who performed the action';