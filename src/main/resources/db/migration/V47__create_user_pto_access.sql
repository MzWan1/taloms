-- ── User ↔ PTO access links ───────────────────────────────────────────────────
-- A row here grants a user access to a proof of residence for a specific PTO.
-- Ownership is derived from the ID-number match on users / pto_records and is
-- NOT stored here — only delegated "linked" access is stored in this table.
CREATE TABLE user_pto_access (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    pto_id      BIGINT      NOT NULL REFERENCES pto_records(id),
    created_by  VARCHAR(50),
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_pto_access UNIQUE (user_id, pto_id)
);

CREATE INDEX idx_user_pto_access_user ON user_pto_access(user_id);
CREATE INDEX idx_user_pto_access_pto  ON user_pto_access(pto_id);