-- Add parcel_id column to pto_records table
ALTER TABLE pto_records ADD COLUMN IF NOT EXISTS parcel_id BIGINT;

-- Add foreign key constraint
ALTER TABLE pto_records ADD CONSTRAINT fk_pto_parcel
    FOREIGN KEY (parcel_id) REFERENCES parcels(id);

-- Create index for faster queries
CREATE INDEX IF NOT EXISTS idx_pto_parcel_id ON pto_records(parcel_id);

-- Add comment for documentation
COMMENT ON COLUMN pto_records.parcel_id IS 'The parcel this PTO is issued for';