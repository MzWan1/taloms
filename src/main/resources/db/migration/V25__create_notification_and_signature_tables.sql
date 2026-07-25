-- V25__create_notification_and_signature_tables.sql
-- Notification module and PTO e-signature support

-- Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(150) NOT NULL,
    subject VARCHAR(255),
    body TEXT,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failed_at TIMESTAMP,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_entity ON notifications(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient);
CREATE INDEX IF NOT EXISTS idx_notifications_channel ON notifications(channel);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);

-- PTO approval signatures table
CREATE TABLE IF NOT EXISTS pto_approval_signatures (
    id BIGSERIAL PRIMARY KEY,
    pto_id BIGINT NOT NULL,
    signed_by VARCHAR(150) NOT NULL,
    signature_data TEXT,
    signature_image_path VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    signed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_signature_pto FOREIGN KEY (pto_id) REFERENCES pto_records(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_signature_pto_id ON pto_approval_signatures(pto_id);
CREATE INDEX IF NOT EXISTS idx_signature_signed_by ON pto_approval_signatures(signed_by);
