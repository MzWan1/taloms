-- V48__create_company_api_tables.sql
-- ── External company API ──────────────────────────────────────────────────────
-- Introduces the COMPANY role, the api-credentialed external partner API and
-- its usage tracking.
--
-- Deliberately SEPARATE from audit_logs:
--   * "companies"          — the external organisation (not tied to a human user)
--   * "company_api_keys"   — hashed API credentials (never stored in plaintext)
--   * "api_usage_logs"     — external API access records (audit_logs is for the
--                            TALOMS user/entity audit trail and must not be used
--                            for API usage tracking)
--
-- No existing column/table is modified by this migration.

-- ── ROLE_COMPANY ──────────────────────────────────────────────────────────────
-- Granted to externally authenticated API callers (and available for a future
-- company self-service login). It carries NO administrative authority.
INSERT INTO roles (name, description)
SELECT 'ROLE_COMPANY', 'External company authorised to use the TALOMS partner API'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_COMPANY');

-- ── Companies ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS companies (
    id                  BIGSERIAL    PRIMARY KEY,
    name                VARCHAR(150) NOT NULL,
    registration_number VARCHAR(50),
    contact_email       VARCHAR(150) NOT NULL,
    contact_phone       VARCHAR(20),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    disabled_reason     TEXT,
    disabled_at         TIMESTAMP,
    created_by          VARCHAR(50)  NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_companies_name UNIQUE (name),
    CONSTRAINT uq_companies_registration_number UNIQUE (registration_number),
    CONSTRAINT chk_companies_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX IF NOT EXISTS idx_companies_status ON companies(status);

-- ── Company API keys ──────────────────────────────────────────────────────────
-- key_hash is the SHA-256 hex digest of the raw key. The raw key is shown once
-- at generation time and never persisted. key_prefix is a short, non-secret
-- identifier so administrators can tell keys apart.
CREATE TABLE IF NOT EXISTS company_api_keys (
    id            BIGSERIAL    PRIMARY KEY,
    company_id    BIGINT       NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    label         VARCHAR(100),
    key_prefix    VARCHAR(24)  NOT NULL,
    key_hash      VARCHAR(64)  NOT NULL,
    scopes        VARCHAR(255) NOT NULL DEFAULT 'POR_READ',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_used_at  TIMESTAMP,
    revoked_at    TIMESTAMP,
    revoked_by    VARCHAR(50),
    revoke_reason VARCHAR(255),
    created_by    VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_company_api_keys_hash UNIQUE (key_hash),
    CONSTRAINT chk_company_api_keys_status CHECK (status IN ('ACTIVE', 'REVOKED'))
);

-- The API authentication lookup is by key_hash, so this index is the hot path.
CREATE INDEX IF NOT EXISTS idx_company_api_keys_company ON company_api_keys(company_id);
CREATE INDEX IF NOT EXISTS idx_company_api_keys_status  ON company_api_keys(status);

-- ── API usage / access logs ───────────────────────────────────────────────────
-- company_id / api_key_id are nullable because a request can fail before a key
-- is identified (e.g. missing key). ON DELETE SET NULL keeps history alive.
-- id_number_hash is a SHA-256 digest used only for correlation: the full ID
-- number is never written to this table.
CREATE TABLE IF NOT EXISTS api_usage_logs (
    id              BIGSERIAL    PRIMARY KEY,
    company_id      BIGINT       REFERENCES companies(id) ON DELETE SET NULL,
    api_key_id      BIGINT       REFERENCES company_api_keys(id) ON DELETE SET NULL,
    endpoint        VARCHAR(255) NOT NULL,
    http_method     VARCHAR(10)  NOT NULL,
    required_scope  VARCHAR(30),
    outcome         VARCHAR(20)  NOT NULL,
    response_status INTEGER      NOT NULL,
    failure_reason  VARCHAR(40),
    id_number_hash  VARCHAR(64),
    client_ip       VARCHAR(45),
    user_agent      VARCHAR(255),
    duration_ms     INTEGER,
    requested_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_api_usage_logs_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE'))
);

CREATE INDEX IF NOT EXISTS idx_api_usage_logs_company      ON api_usage_logs(company_id);
CREATE INDEX IF NOT EXISTS idx_api_usage_logs_api_key      ON api_usage_logs(api_key_id);
CREATE INDEX IF NOT EXISTS idx_api_usage_logs_requested_at ON api_usage_logs(requested_at);
CREATE INDEX IF NOT EXISTS idx_api_usage_logs_id_hash      ON api_usage_logs(id_number_hash);
