-- V51__create_chief_authorities_join_table.sql
-- ── Chief ↔ Traditional Authority (many-to-many) ──────────────────────────────
-- A Chief (users row holding ROLE_CHIEF) may now belong to one or more
-- Traditional Authorities, and an Authority may have one or more Chiefs.
--
-- The existing one-to-one links are preserved by this migration:
--   * users.traditional_authority_id  (chief/headman side)
--   * traditional_authorities.chief_id (authority side)
-- Both are copied into the new join table BEFORE anything else changes, so no
-- existing Chief/Authority association can be lost.
--
-- NOTE: users.traditional_authority_id is deliberately NOT dropped. It remains
-- the authoritative single-authority link for HEADSMAN users (a headsman may
-- only serve one authority — an existing, unrelated business rule).

-- ── Join table ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS chief_authorities (
    chief_id     BIGINT    NOT NULL REFERENCES users(id)                  ON DELETE CASCADE,
    authority_id BIGINT    NOT NULL REFERENCES traditional_authorities(id) ON DELETE CASCADE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_chief_authorities PRIMARY KEY (chief_id, authority_id)
);

CREATE INDEX IF NOT EXISTS idx_chief_authorities_authority
    ON chief_authorities(authority_id);
CREATE INDEX IF NOT EXISTS idx_chief_authorities_chief
    ON chief_authorities(chief_id);

-- ── Migrate existing associations (chief side) ────────────────────────────────
-- Every existing CHIEF user that is linked to an authority via the legacy
-- users.traditional_authority_id column becomes a row in the join table.
INSERT INTO chief_authorities (chief_id, authority_id)
SELECT DISTINCT u.id, u.traditional_authority_id
FROM   users u
JOIN   user_roles ur ON ur.user_id = u.id
JOIN   roles      r  ON r.id = ur.role_id AND r.name = 'ROLE_CHIEF'
WHERE  u.traditional_authority_id IS NOT NULL
ON CONFLICT (chief_id, authority_id) DO NOTHING;

-- ── Migrate existing associations (authority side) ────────────────────────────
-- Every authority that names a chief via traditional_authorities.chief_id also
-- becomes a join-table row, covering any link that only existed on that side.
INSERT INTO chief_authorities (chief_id, authority_id)
SELECT DISTINCT a.chief_id, a.id
FROM   traditional_authorities a
WHERE  a.chief_id IS NOT NULL
ON CONFLICT (chief_id, authority_id) DO NOTHING;
