-- ── PTO Records ───────────────────────────────────────────────────────────────
CREATE TABLE pto_records (
                             id                  BIGSERIAL       PRIMARY KEY,
                             pto_number          VARCHAR(20)     NOT NULL UNIQUE,
                             pto_holder_name     VARCHAR(150)    NOT NULL,
                             id_number           VARCHAR(13)     NOT NULL,
                             contact_phone       VARCHAR(20),
                             contact_email       VARCHAR(150),
                             purpose             VARCHAR(30)     NOT NULL,
                             status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
                             issue_date          DATE            NOT NULL,
                             expiry_date         DATE,
                             notes               TEXT,
                             village_id          BIGINT          REFERENCES villages(id),
                             traditional_authority_id BIGINT     REFERENCES traditional_authorities(id),
                             approved_by         VARCHAR(50),
                             approved_at         TIMESTAMP,
                             revoked_by          VARCHAR(50),
                             revoked_at          TIMESTAMP,
                             revoke_reason       TEXT,
                             created_by          VARCHAR(50),
                             created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
                             updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ── Indexes ───────────────────────────────────────────────────────────────────
CREATE INDEX idx_pto_number      ON pto_records(pto_number);
CREATE INDEX idx_pto_status      ON pto_records(status);
CREATE INDEX idx_pto_id_number   ON pto_records(id_number);
CREATE INDEX idx_pto_village     ON pto_records(village_id);
CREATE INDEX idx_pto_authority   ON pto_records(traditional_authority_id);
CREATE INDEX idx_pto_holder_name ON pto_records(pto_holder_name);