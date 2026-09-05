-- ── Link Users to Traditional Authorities ──────────────────────────────────
-- Allows Chief users to be associated with a specific Traditional Authority
-- so they can only manage villages within their own authority.

ALTER TABLE users
    ADD COLUMN traditional_authority_id BIGINT
        REFERENCES traditional_authorities(id);

CREATE INDEX idx_user_traditional_authority
    ON users(traditional_authority_id);
