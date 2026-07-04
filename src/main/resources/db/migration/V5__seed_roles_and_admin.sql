-- ── Insert Roles ──────────────────────────────────────────────────────────────
INSERT INTO roles (name, description) VALUES
                                          ('ROLE_SYSTEM_ADMIN',    'Full system access'),
                                          ('ROLE_TA_ADMINISTRATOR','Traditional Authority administrator'),
                                          ('ROLE_LAND_OFFICER',    'Land demarcation and parcel management'),
                                          ('ROLE_DATA_CAPTURER',   'Household and resident data capture'),
                                          ('ROLE_REPORT_VIEWER',   'Read-only report access');

-- ── Insert Default Admin User ─────────────────────────────────────────────────
-- Password is: Admin@1234
-- BCrypt hash generated with strength 12
INSERT INTO users (username, email, password_hash, full_name, enabled)
VALUES (
           'admin',
           'admin@taloms.co.za',
           '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
           'System Administrator',
           TRUE
       );

-- ── Assign SYSTEM_ADMIN role to admin user ────────────────────────────────────
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM   users u, roles r
WHERE  u.username = 'admin'
  AND    r.name     = 'ROLE_SYSTEM_ADMIN';