package za.co.taloms.company.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.company.application.dto.ApiUsageLogResponse;
import za.co.taloms.company.application.dto.ApiUsageRecord;
import za.co.taloms.company.domain.entity.ApiUsageLog;
import za.co.taloms.company.domain.repository.ApiUsageLogRepositoryPort;
import za.co.taloms.company.domain.repository.CompanyApiKeyRepositoryPort;
import za.co.taloms.company.domain.repository.CompanyRepositoryPort;

import java.util.HashMap;
import java.util.List;
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
    public List<ApiUsageLogResponse> findAll() {
        return toResponses(usageRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiUsageLogResponse> findByCompany(Long companyId) {
        return toResponses(usageRepository.findByCompanyId(companyId));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCompany(Long companyId) {
        return usageRepository.countByCompanyId(companyId);
    }

    private List<ApiUsageLogResponse> toResponses(List<ApiUsageLog> logs) {
        // Resolve company names / key prefixes once per distinct id to avoid N+1.
        Map<Long, String> companyNames = new HashMap<>();
        Map<Long, String> keyPrefixes = new HashMap<>();

        return logs.stream().map(log -> {
            String companyName = null;
            if (log.getCompanyId() != null) {
                companyName = companyNames.computeIfAbsent(log.getCompanyId(),
                        id -> companyRepository.findById(id).map(c -> c.getName()).orElse(null));
            }
            String keyPrefix = null;
            if (log.getApiKeyId() != null) {
                keyPrefix = keyPrefixes.computeIfAbsent(log.getApiKeyId(),
                        id -> apiKeyRepository.findById(id).map(k -> k.getKeyPrefix()).orElse(null));
            }
            return ApiUsageLogResponse.builder()
                    .id(log.getId())
                    .companyId(log.getCompanyId())
                    .companyName(companyName)
                    .apiKeyId(log.getApiKeyId())
                    .apiKeyPrefix(keyPrefix)
                    .endpoint(log.getEndpoint())
                    .httpMethod(log.getHttpMethod())
                    .requiredScope(log.getRequiredScope())
                    .outcome(log.getOutcome())
                    .responseStatus(log.getResponseStatus())
                    .failureReason(log.getFailureReason())
                    .failureReasonDisplay(log.getFailureReason() == null
                            ? null
                            : log.getFailureReason().name().replace('_', ' '))
                    .idNumberHash(log.getIdNumberHash())
                    .clientIp(log.getClientIp())
                    .userAgent(log.getUserAgent())
                    .durationMs(log.getDurationMs())
                    .requestedAt(log.getRequestedAt())
                    .build();
        }).toList();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}