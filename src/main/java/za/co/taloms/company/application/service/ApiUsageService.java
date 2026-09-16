package za.co.taloms.company.application.service;

import za.co.taloms.company.application.dto.ApiUsageLogResponse;
import za.co.taloms.company.application.dto.ApiUsageRecord;

import java.util.List;

/**
 * API usage tracking — a dedicated concern, separate from the TALOMS audit trail
 * ({@code audit_logs}). Writing must never break the API request it describes.
 */
public interface ApiUsageService {

    /** Persists one usage record. Never throws into the calling request. */
    void record(ApiUsageRecord record);

    /** All usage records, newest first (ADMIN only). */
    List<ApiUsageLogResponse> findAll();

    /** Usage records for one company, newest first. */
    List<ApiUsageLogResponse> findByCompany(Long companyId);

    long countByCompany(Long companyId);
}