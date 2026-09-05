-- ── Add chief_id and headman_id to authorities and villages ────────────────
-- Stores user references for Chief and Headman instead of just names.

ALTER TABLE traditional_authorities
    ADD COLUMN chief_id BIGINT REFERENCES users(id);

ALTER TABLE traditional_authorities
    ADD COLUMN headman_id BIGINT REFERENCES users(id);

ALTER TABLE villages
    ADD COLUMN headman_id BIGINT REFERENCES users(id);

CREATE INDEX idx_authority_chief ON traditional_authorities(chief_id);
CREATE INDEX idx_authority_headman ON traditional_authorities(headman_id);
CREATE INDEX idx_village_headman ON villages(headman_id);
