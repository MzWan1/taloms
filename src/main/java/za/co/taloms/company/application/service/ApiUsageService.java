package za.co.taloms.company.application.service;

import za.co.taloms.common.PageResponse;
import za.co.taloms.company.application.dto.ApiUsageLogResponse;
import za.co.taloms.company.application.dto.ApiUsageRecord;

/**
 * API usage tracking — a dedicated concern, separate from the TALOMS audit trail
 * ({@code audit_logs}). Writing must never break the API request it describes.
 */
public interface ApiUsageService {

    /** Persists one usage record. Never throws into the calling request. */
    void record(ApiUsageRecord record);

    /**
     * One page of a company's usage records, newest first. Usage logs grow
     * without bound, so reads are ALWAYS paginated — never an unbounded list.
     */
    PageResponse<ApiUsageLogResponse> findByCompany(Long companyId, Integer page, Integer pageSize);

    long countByCompany(Long companyId);
}
