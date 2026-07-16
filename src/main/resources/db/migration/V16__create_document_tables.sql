-- V16__create_document_tables.sql
-- Document Management tables for file uploads and storage

-- Documents table
CREATE TABLE IF NOT EXISTS documents (
                                         id BIGSERIAL PRIMARY KEY,
                                         original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    related_entity_type VARCHAR(50) NOT NULL,
    related_entity_id BIGINT NOT NULL,
    description TEXT,
    uploaded_by VARCHAR(50),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    version INTEGER DEFAULT 1,
    checksum VARCHAR(64),
    notes TEXT,

    CONSTRAINT chk_file_size CHECK (file_size <= 20971520) -- 20MB max
    );

-- Create indexes for faster queries
CREATE INDEX idx_documents_related_entity ON documents(related_entity_type, related_entity_id);
CREATE INDEX idx_documents_document_type ON documents(document_type);
CREATE INDEX idx_documents_uploaded_by ON documents(uploaded_by);
CREATE INDEX idx_documents_uploaded_at ON documents(uploaded_at);
CREATE INDEX idx_documents_active ON documents(active);

-- Create trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION update_document_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW
    EXECUTE FUNCTION update_document_updated_at();

-- Document Access Log table (for POPIA compliance)
CREATE TABLE IF NOT EXISTS document_access_logs (
                                                    id BIGSERIAL PRIMARY KEY,
                                                    document_id BIGINT NOT NULL,
                                                    accessed_by VARCHAR(50) NOT NULL,
    access_type VARCHAR(30) NOT NULL, -- DOWNLOAD, VIEW, DELETE
    access_ip VARCHAR(45),
    user_agent TEXT,
    accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_doc_access_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
    );

-- Create indexes for access logs
CREATE INDEX idx_doc_access_document_id ON document_access_logs(document_id);
CREATE INDEX idx_doc_access_accessed_by ON document_access_logs(accessed_by);
CREATE INDEX idx_doc_access_accessed_at ON document_access_logs(accessed_at);

-- Comments for documentation
COMMENT ON TABLE documents IS 'Uploaded documents with versioning and metadata';
COMMENT ON TABLE document_access_logs IS 'Audit trail for document access (POPIA compliance)';
COMMENT ON COLUMN documents.document_type IS 'PTO_CERT, ID_COPY, SURVEY, PHOTO, OTHER';
COMMENT ON COLUMN documents.related_entity_type IS 'PTO, PARCEL, RESIDENT, HOUSEHOLD, BUSINESS';
COMMENT ON COLUMN documents.stored_filename IS 'Unique system-generated filename for storage';
COMMENT ON COLUMN documents.checksum IS 'SHA-256 checksum for integrity verification';