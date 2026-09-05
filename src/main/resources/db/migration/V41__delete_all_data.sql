-- V41__delete_all_data.sql
-- Delete all data from the database and recreate admin user

TRUNCATE TABLE
    role_permissions,
    user_roles,
    users,
    roles,
    permissions,
    traditional_authorities,
    villages,
    report_schedules,
    audit_logs,
    parcel_boundaries,
    parcels,
    pto_records,
    business_occupancies,
    residents,
    households,
    gis_layers,
    password_reset_tokens,
    notifications,
    pto_approval_signatures,
    document_access_logs,
    documents
CASCADE;

-- Recreate the 3 roles
INSERT INTO roles (name, description) VALUES
    ('ROLE_ADMIN', 'System administrator with full access'),
    ('ROLE_CHIEF', 'Chief managing an assigned authority'),
    ('ROLE_HEADSMAN', 'Headsman handling day-to-day operations');

-- Recreate admin user (password: Admin@1234)
INSERT INTO users (username, email, password_hash, full_name, enabled)
VALUES (
    'admin',
    'admin@taloms.co.za',
    '$2a$12$kKvWu75tb8bMXEyLv.4SO.9A8QyukMUBsQJ6eQZH6D36ntjTY0M0q',
    'System Administrator',
    TRUE
);

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';
