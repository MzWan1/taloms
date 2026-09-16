-- ── ROLE_USER (external resident / proof-of-residence access) ────────────────
INSERT INTO roles (name, description)
VALUES ('ROLE_USER', 'Registered citizen/resident with self-service access to proofs of residence for PTOs linked to them');

-- ── Personal ID number on users ───────────────────────────────────────────────
-- Used to derive PTO ownership: a user "owns" a PTO when their SA ID number
-- matches the PTO holder's id_number.
ALTER TABLE users ADD COLUMN id_number VARCHAR(13) UNIQUE;

CREATE INDEX idx_users_id_number ON users(id_number);