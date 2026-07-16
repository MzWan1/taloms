-- test-data.sql
-- Seed roles for testing (H2 compatible)

-- Delete existing data
DELETE FROM role_permissions;
DELETE FROM permissions;
DELETE FROM roles;

-- Insert roles
INSERT INTO roles (name, description, created_at, updated_at) VALUES
                                                                  ('ROLE_SYSTEM_ADMIN', 'System Administrator', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                  ('ROLE_TA_ADMINISTRATOR', 'TA Administrator', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                  ('ROLE_LAND_OFFICER', 'Land Officer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                  ('ROLE_DATA_CAPTURER', 'Data Capturer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                  ('ROLE_REPORT_VIEWER', 'Report Viewer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert permissions
INSERT INTO permissions (name, description, created_at, updated_at) VALUES
                                                                        ('PTO_CREATE', 'Create PTOs', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                        ('PTO_APPROVE', 'Approve PTOs', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                        ('PTO_REVOKE', 'Revoke PTOs', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                        ('USER_MANAGE', 'Manage Users', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                        ('PARCEL_CREATE', 'Create Parcels', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                        ('REPORT_VIEW', 'View Reports', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);