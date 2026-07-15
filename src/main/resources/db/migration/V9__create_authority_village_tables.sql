-- ── Traditional Authorities ───────────────────────────────────────────────────
CREATE TABLE traditional_authorities (
                                         id               BIGSERIAL    PRIMARY KEY,
                                         authority_name   VARCHAR(150) NOT NULL UNIQUE,
                                         chief_name       VARCHAR(150) NOT NULL,
                                         headman_name     VARCHAR(150),
                                         contact_phone    VARCHAR(20),
                                         contact_email    VARCHAR(150),
                                         physical_address VARCHAR(255),
                                         region           VARCHAR(100),
                                         active           BOOLEAN      NOT NULL DEFAULT TRUE,
                                         created_by       VARCHAR(50),
                                         created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
                                         updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ── Villages ─────────────────────────────────────────────────────────────────
CREATE TABLE villages (
                          id                      BIGSERIAL    PRIMARY KEY,
                          village_name            VARCHAR(150) NOT NULL,
                          region                  VARCHAR(100),
                          headman_name            VARCHAR(150),
                          description             TEXT,
                          active                  BOOLEAN      NOT NULL DEFAULT TRUE,
                          traditional_authority_id BIGINT      NOT NULL
                              REFERENCES traditional_authorities(id),
                          created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
                          updated_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
                          UNIQUE (village_name, traditional_authority_id)
);

-- ── Indexes ───────────────────────────────────────────────────────────────────
CREATE INDEX idx_authority_name   ON traditional_authorities(authority_name);
CREATE INDEX idx_authority_active ON traditional_authorities(active);
CREATE INDEX idx_village_authority ON villages(traditional_authority_id);
CREATE INDEX idx_village_active    ON villages(active);