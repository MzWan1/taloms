-- V52__performance_resilience_indexes.sql
-- ── Production-readiness pass ─────────────────────────────────────────────────
-- Adds ONLY additive performance / integrity objects. No existing column, table
-- or row is modified, so this migration is safe to replay on any environment.
--
-- 1. Composite indexes for real query patterns (verify before adding):
--    * PTO sync delta           — p.updated_at >= :since ORDER BY p.updated_at
--    * PTO active-proof lookup  — id_number + status (PoR verify hot path)
--    * PTO dashboard pending    — status + created_at (findByStatus ORDER BY)
--    * Documents newest-first   — uploaded_at DESC (findAllOrderByUploadedAtDesc)
--    * Document access history  — document_id + accessed_at DESC
--    * API usage newest-first   — company_id + requested_at DESC
--                                 (self-service + admin usage views; the single
--                                 column indexes from V48 remain for other paths)
-- 2. Partial UNIQUE constraint for duplicate-submission protection:
--    one PENDING (or SUSPENDED) PTO per (parcel, holder). Approved/revoked/
--    expired rows are excluded, matching the existing business rule enforced
--    in PTOServiceImpl (a parcel may hold exactly one live PTO).
-- 3. Request correlation: X-Request-Id support written to application logs.
--    The header is read from the incoming request or generated server-side; it
--    is never persisted, contains no personal data, and is safe to expose.

CREATE INDEX IF NOT EXISTS idx_pto_updated_at
    ON pto_records(updated_at);

CREATE INDEX IF NOT EXISTS idx_pto_id_number_status
    ON pto_records(id_number, status);

CREATE INDEX IF NOT EXISTS idx_pto_status_created_at
    ON pto_records(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_documents_uploaded_at_desc
    ON documents(uploaded_at DESC);

CREATE INDEX IF NOT EXISTS idx_doc_access_document_accessed
    ON document_access_logs(document_id, accessed_at DESC);

CREATE INDEX IF NOT EXISTS idx_api_usage_company_requested
    ON api_usage_logs(company_id, requested_at DESC);

-- Duplicate-submission protection (server-side, race-free):
-- at most ONE live (PENDING or SUSPENDED) PTO per parcel per holder ID.
CREATE UNIQUE INDEX IF NOT EXISTS uq_pto_live_parcel_holder
    ON pto_records(parcel_id, id_number)
    WHERE (status IN ('PENDING', 'SUSPENDED'));