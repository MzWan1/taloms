-- V56: ensure API usage search fields exist.
ALTER TABLE api_usage_logs ADD COLUMN IF NOT EXISTS search_type varchar;
ALTER TABLE api_usage_logs ADD COLUMN IF NOT EXISTS masked_search_value varchar;
