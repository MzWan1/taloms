-- ── Authority & Village boundaries (JSON array of {lat, lng} vertices) ──────
ALTER TABLE traditional_authorities
    ADD COLUMN boundary_json TEXT;

ALTER TABLE villages
    ADD COLUMN boundary_json TEXT;