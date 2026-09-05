-- ── Insert Admin Role ─────────────────────────────────────────────────────────
INSERT INTO roles (name, description) VALUES
                                          ('ROLE_ADMIN', 'System administrator with full access');

-- ── Insert Default Admin User ─────────────────────────────────────────────────
-- Password is: Admin@1234
INSERT INTO users (username, email, password_hash, full_name, enabled)
VALUES (
           'admin',
           'admin@taloms.co.za',
           '$2a$12$kKvWu75tb8bMXEyLv.4SO.9A8QyukMUBsQJ6eQZH6D36ntjTY0M0q',
           'System Administrator',
           TRUE
       );

-- ── Assign ADMIN role to admin user ───────────────────────────────────────────
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM   users u, roles r
WHERE  u.username = 'admin'
  AND    r.name     = 'ROLE_ADMIN';