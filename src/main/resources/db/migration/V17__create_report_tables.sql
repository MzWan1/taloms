-- V17__create_report_tables.sql
-- Report scheduling and configuration (Phase 2 - Future enhancement)

CREATE TABLE IF NOT EXISTS report_schedules (
                                                id BIGSERIAL PRIMARY KEY,
                                                report_type VARCHAR(50) NOT NULL,
    report_name VARCHAR(200) NOT NULL,
    schedule_type VARCHAR(20) NOT NULL, -- DAILY, WEEKLY, MONTHLY
    schedule_config JSONB,
    format VARCHAR(10) NOT NULL DEFAULT 'PDF',
    recipient_email VARCHAR(200) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_run_at TIMESTAMP,
    next_run_at TIMESTAMP,

    CONSTRAINT chk_report_format CHECK (format IN ('PDF', 'EXCEL'))
    );

-- Comment for documentation
COMMENT ON TABLE report_schedules IS 'Scheduled report configurations (Future enhancement)';
COMMENT ON COLUMN report_schedules.report_type IS 'PTO, PARCEL, POPULATION, LAND_UTILISATION';