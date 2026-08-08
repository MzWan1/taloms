-- V36__make_business_pto_nullable.sql
-- Allow business occupancies to be created without a PTO linkage.
-- PTO is now optional at creation time and can be linked later.
-- See: Household/Business module PTO linking dependency flaw resolution.

ALTER TABLE business_occupancies ALTER COLUMN pto_id DROP NOT NULL;

COMMENT ON COLUMN business_occupancies.pto_id IS 'The PTO associated with this business (optional at creation; can be linked later)';
