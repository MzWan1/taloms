-- V22: Sanitize household data for safe template rendering
-- This guards against production data issues that can cause 500s in Thymeleaf.
-- Safe to re-run: all statements are idempotent.

-- 1) Ensure active is never null
UPDATE households
SET active = true
WHERE active IS NULL;

-- 2) Ensure registration_date is never null
UPDATE households
SET registration_date = CURRENT_DATE
WHERE registration_date IS NULL;

-- 3) Ensure household_head_name is never blank for existing rows
-- (schema already enforces NOT NULL, but legacy inserts/backfills may have empty strings)
UPDATE households
SET household_head_name = 'Unknown'
WHERE household_head_name IS NULL OR TRIM(household_head_name) = '';

-- 4) Ensure household_head_id_number is never blank for existing rows
UPDATE households
SET household_head_id_number = 'N/A'
WHERE household_head_id_number IS NULL OR TRIM(household_head_id_number) = '';
