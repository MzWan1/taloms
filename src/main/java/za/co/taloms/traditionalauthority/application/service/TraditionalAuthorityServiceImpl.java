package za.co.taloms.traditionalauthority.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.DuplicateRecordException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.traditionalauthority.application.dto.*;
import za.co.taloms.traditionalauthority.domain.entity.TraditionalAuthority;
import za.co.taloms.traditionalauthority.domain.repository.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TraditionalAuthorityServiceImpl
        implements TraditionalAuthorityService {

    private final TraditionalAuthorityRepositoryPort authorityRepository;
    private final VillageRepositoryPort              villageRepository;

    @Override
    public TraditionalAuthorityResponse create(
            TraditionalAuthorityRequest request, String createdBy) {

        if (authorityRepository.existsByAuthorityName(
                request.getAuthorityName())) {
            throw new DuplicateRecordException(
                    "Traditional Authority",
                    "name",
                    request.getAuthorityName());
        }

        var authority = TraditionalAuthority.builder()
                .authorityName(request.getAuthorityName())
                .chiefName(request.getChiefName())
                .headmanName(request.getHeadmanName())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .physicalAddress(request.getPhysicalAddress())
                .region(request.getRegion())
                .active(true)
                .createdBy(createdBy)
                .build();

        var saved = authorityRepository.save(authority);
        log.info("Created Traditional Authority: {} by {}",
                saved.getAuthorityName(), createdBy);
        return toResponse(saved);
    }

    @Override
    public TraditionalAuthorityResponse update(
            Long id, TraditionalAuthorityRequest request) {

        var authority = authorityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Traditional Authority", id));

        if (authorityRepository.existsByAuthorityNameAndIdNot(
                request.getAuthorityName(), id)) {
            throw new DuplicateRecordException(
                    "Traditional Authority",
                    "name",
                    request.getAuthorityName());
        }

        authority.setAuthorityName(request.getAuthorityName());
        authority.setChiefName(request.getChiefName());
        authority.setHeadmanName(request.getHeadmanName());
        authority.setContactPhone(request.getContactPhone());
        authority.setContactEmail(request.getContactEmail());
        authority.setPhysicalAddress(request.getPhysicalAddress());
        authority.setRegion(request.getRegion());

        var saved = authorityRepository.save(authority);
        log.info("Updated Traditional Authority: {}", saved.getAuthorityName());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TraditionalAuthorityResponse findById(Long id) {
        return authorityRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Traditional Authority", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TraditionalAuthorityResponse> findAll() {
        return authorityRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TraditionalAuthorityResponse> findAllActive() {
        return authorityRepository.findAllActive()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TraditionalAuthorityResponse> searchByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return findAll();
        }
        return authorityRepository.findAll().stream()
                .filter(a -> a.getAuthorityName().toLowerCase().contains(name.trim().toLowerCase())
                        || (a.getChiefName() != null && a.getChiefName().toLowerCase().contains(name.trim().toLowerCase()))
                        || (a.getRegion() != null && a.getRegion().toLowerCase().contains(name.trim().toLowerCase())))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deactivate(Long id) {
        var authority = authorityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Traditional Authority", id));
        authority.setActive(false);
        authorityRepository.save(authority);
        log.info("Deactivated Traditional Authority: {}",
                authority.getAuthorityName());
    }

    @Override
    public void activate(Long id) {
        var authority = authorityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Traditional Authority", id));
        authority.setActive(true);
        authorityRepository.save(authority);
        log.info("Activated Traditional Authority: {}",
                authority.getAuthorityName());
    }

    private TraditionalAuthorityResponse toResponse(
            TraditionalAuthority a) {
        long villageCount = villageRepository
                .countByTraditionalAuthorityId(a.getId());
        return TraditionalAuthorityResponse.builder()
                .id(a.getId())
                .authorityName(a.getAuthorityName())
                .chiefName(a.getChiefName())
                .headmanName(a.getHeadmanName())
                .contactPhone(a.getContactPhone())
                .contactEmail(a.getContactEmail())
                .physicalAddress(a.getPhysicalAddress())
                .region(a.getRegion())
                .active(a.getActive())
                .villageCount(villageCount)
                .createdBy(a.getCreatedBy())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}