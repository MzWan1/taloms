-- V50__link_users_to_companies.sql
-- ── Link users with ROLE_COMPANY to their company organisation ─────────────────
-- This enables company self-service login: a user with ROLE_COMPANY can log in
-- via form authentication and access their company's profile, API keys and usage.
-- The column is nullable because most users (ADMIN, CHIEF, HEADSMAN, USER) are
-- not associated with a company organisation.

ALTER TABLE users
    ADD COLUMN company_id BIGINT REFERENCES companies(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_users_company_id ON users(company_id);

-- Link existing company users to their companies by matching username to created_by
-- This is a best-effort backfill: adjust as needed for your data.
-- UPDATE users u
-- SET    company_id = c.id
-- FROM   companies c
-- WHERE  u.username = c.created_by
-- AND    u.company_id IS NULL;
