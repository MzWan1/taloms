-- V27__add_document_content_blob_column.sql
-- Store document file content in database as BYTEA instead of filesystem
-- Fixes ephemeral filesystem issues on Render.com and other cloud platforms

ALTER TABLE documents ADD COLUMN IF NOT EXISTS content BYTEA;
