-- ── Roles ─────────────────────────────────────────────────────────────────────
CREATE TABLE roles (
                       id          BIGSERIAL PRIMARY KEY,
                       name        VARCHAR(50)  NOT NULL UNIQUE,
                       description VARCHAR(255),
                       created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ── Permissions ───────────────────────────────────────────────────────────────
CREATE TABLE permissions (
                             id          BIGSERIAL PRIMARY KEY,
                             name        VARCHAR(100) NOT NULL UNIQUE,
                             description VARCHAR(255),
                             created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ── Role Permissions (join table) ─────────────────────────────────────────────
CREATE TABLE role_permissions (
                                  role_id       BIGINT NOT NULL REFERENCES roles(id),
                                  permission_id BIGINT NOT NULL REFERENCES permissions(id),
                                  PRIMARY KEY (role_id, permission_id)
);

-- ── Users ─────────────────────────────────────────────────────────────────────
CREATE TABLE users (
                       id                    BIGSERIAL    PRIMARY KEY,
                       username              VARCHAR(50)  NOT NULL UNIQUE,
                       email                 VARCHAR(150) NOT NULL UNIQUE,
                       password_hash         VARCHAR(255) NOT NULL,
                       full_name             VARCHAR(150),
                       enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
                       account_locked        BOOLEAN      NOT NULL DEFAULT FALSE,
                       failed_login_attempts INT          NOT NULL DEFAULT 0,
                       last_login_at         TIMESTAMP,
                       created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
                       updated_at            TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ── User Roles (join table) ───────────────────────────────────────────────────
CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL REFERENCES users(id),
                            role_id BIGINT NOT NULL REFERENCES roles(id),
                            PRIMARY KEY (user_id, role_id)
);

-- ── Indexes ───────────────────────────────────────────────────────────────────
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_users_enabled  ON users(enabled);