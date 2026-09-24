package za.co.taloms.company.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.common.PageResponse;
import za.co.taloms.common.pagination.PageRequestUtils;
import za.co.taloms.company.application.dto.ApiUsageLogResponse;
import za.co.taloms.company.application.dto.ApiUsageRecord;
import za.co.taloms.company.domain.entity.ApiUsageLog;
import za.co.taloms.company.domain.repository.ApiUsageLogRepositoryPort;
import za.co.taloms.company.domain.repository.CompanyApiKeyRepositoryPort;
import za.co.taloms.company.domain.repository.CompanyRepositoryPort;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiUsageServiceImpl implements ApiUsageService {

    private static final int MAX_FIELD_LENGTH = 255;

    private final ApiUsageLogRepositoryPort usageRepository;
    private final CompanyRepositoryPort companyRepository;
    private final CompanyApiKeyRepositoryPort apiKeyRepository;

    /**
     * Written in its own transaction so a usage record survives even if the
     * request that produced it failed. Failures are swallowed (and logged at
     * debug) because usage logging must never break API availability.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(ApiUsageRecord record) {
        try {
            if (record == null) {
                return;
            }
            usageRepository.save(ApiUsageLog.builder()
                    .companyId(record.getCompanyId())
                    .apiKeyId(record.getApiKeyId())
                    .endpoint(truncate(record.getEndpoint(), MAX_FIELD_LENGTH))
                    .httpMethod(truncate(record.getHttpMethod(), 10))
                    .requiredScope(truncate(record.getRequiredScope(), 30))
                    .outcome(record.getOutcome())
                    .responseStatus(record.getResponseStatus() == null ? 500 : record.getResponseStatus())
                    .failureReason(record.getFailureReason())
                    .searchType(truncate(record.getSearchType(), 50))
                    .maskedSearchValue(truncate(record.getMaskedSearchValue(), 255))
                    .idNumberHash(truncate(record.getIdNumberHash(), 64))
                    .clientIp(truncate(record.getClientIp(), 45))
                    .userAgent(truncate(record.getUserAgent(), MAX_FIELD_LENGTH))
                    .durationMs(record.getDurationMs())
                    .build());
        } catch (Exception e) {
            // Never leak the record contents (which may include an ID hash).
            log.debug("Failed to persist API usage record for endpoint {}", record.getEndpoint());
            log.trace("API usage logging failure", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ApiUsageLogResponse> findByCompany(Long companyId, Integer page, Integer pageSize) {
        var pageable = PageRequestUtils.toPageable(page, pageSize,
                za.co.taloms.common.ApplicationConstants.DEFAULT_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "requestedAt"));
        Page<ApiUsageLog> logs = usageRepository.findByCompanyId(companyId, pageable);

        // Resolve display names once per DISTINCT id for the whole page
        // (batched per page, not per row).
        Map<Long, String> companyNames = new HashMap<>();
        Map<Long, String> keyPrefixes = new HashMap<>();

        var responses = logs.getContent().stream()
                .map(entry -> toResponse(entry, companyNames, keyPrefixes))
                .toList();

        return PageResponse.<ApiUsageLogResponse>builder()
                .content(responses)
                .pageNumber(logs.getNumber() + 1)
                .pageSize(logs.getSize())
                .totalElements(logs.getTotalElements())
                .totalPages(logs.getTotalPages())
                .last(logs.isLast())
                .build();
    }

    private ApiUsageLogResponse toResponse(ApiUsageLog entry,
            Map<Long, String> companyNames,
            Map<Long, String> keyPrefixes) {
        String companyName = null;
        if (entry.getCompanyId() != null) {
            companyName = companyNames.computeIfAbsent(entry.getCompanyId(),
                    id -> companyRepository.findById(id).map(c -> c.getName()).orElse(null));
        }
        String keyPrefix = null;
        if (entry.getApiKeyId() != null) {
            keyPrefix = keyPrefixes.computeIfAbsent(entry.getApiKeyId(),
                    id -> apiKeyRepository.findById(id).map(k -> k.getKeyPrefix()).orElse(null));
        }
        return ApiUsageLogResponse.builder()
                .id(entry.getId())
                .companyId(entry.getCompanyId())
                .companyName(companyName)
                .apiKeyId(entry.getApiKeyId())
                .apiKeyPrefix(keyPrefix)
                .endpoint(entry.getEndpoint())
                .httpMethod(entry.getHttpMethod())
                .requiredScope(entry.getRequiredScope())
                .outcome(entry.getOutcome())
                .responseStatus(entry.getResponseStatus())
                .failureReason(entry.getFailureReason())
                .failureReasonDisplay(entry.getFailureReason() == null
                        ? null
                        : entry.getFailureReason().name().replace('_', ' '))
                .searchType(entry.getSearchType())
                .maskedSearchValue(entry.getMaskedSearchValue())
                .idNumberHash(entry.getIdNumberHash())
                .clientIp(entry.getClientIp())
                .userAgent(entry.getUserAgent())
                .durationMs(entry.getDurationMs())
                .requestedAt(entry.getRequestedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCompany(Long companyId) {
        return usageRepository.countByCompanyId(companyId);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}