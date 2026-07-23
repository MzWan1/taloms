package za.co.taloms.traditionalauthority.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.common.DuplicateRecordException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.traditionalauthority.application.dto.*;
import za.co.taloms.traditionalauthority.domain.entity.Village;
import za.co.taloms.traditionalauthority.domain.repository.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VillageServiceImpl implements VillageService {

    private final VillageRepositoryPort              villageRepository;
    private final TraditionalAuthorityRepositoryPort authorityRepository;

    @Override
    public VillageResponse create(VillageRequest request) {

        var authority = authorityRepository
                .findById(request.getTraditionalAuthorityId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Traditional Authority",
                        request.getTraditionalAuthorityId()));

        if (villageRepository
                .existsByVillageNameAndTraditionalAuthorityId(
                        request.getVillageName(),
                        request.getTraditionalAuthorityId())) {
            throw new DuplicateRecordException(
                    "Village", "name", request.getVillageName());
        }

        var village = Village.builder()
                .villageName(request.getVillageName())
                .region(request.getRegion())
                .headmanName(request.getHeadmanName())
                .description(request.getDescription())
                .active(true)
                .traditionalAuthority(authority)
                .build();

        var saved = villageRepository.save(village);
        log.info("Created Village: {} under {}",
                saved.getVillageName(),
                authority.getAuthorityName());
        return toResponse(saved);
    }

    @Override
    public VillageResponse update(Long id, VillageRequest request) {

        var village = villageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Village", id));

        var authority = authorityRepository
                .findById(request.getTraditionalAuthorityId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Traditional Authority",
                        request.getTraditionalAuthorityId()));

        if (villageRepository
                .existsByVillageNameAndTraditionalAuthorityIdAndIdNot(
                        request.getVillageName(),
                        request.getTraditionalAuthorityId(),
                        id)) {
            throw new DuplicateRecordException(
                    "Village", "name", request.getVillageName());
        }

        village.setVillageName(request.getVillageName());
        village.setRegion(request.getRegion());
        village.setHeadmanName(request.getHeadmanName());
        village.setDescription(request.getDescription());
        village.setTraditionalAuthority(authority);

        return toResponse(villageRepository.save(village));
    }

    @Override
    @Transactional(readOnly = true)
    public VillageResponse findById(Long id) {
        return villageRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Village", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VillageResponse> findAll() {
        return villageRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VillageResponse> findByAuthority(Long authorityId) {
        return villageRepository
                .findByTraditionalAuthorityId(authorityId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VillageResponse> searchByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return findAll();
        }
        return villageRepository.findAll().stream()
                .filter(v -> v.getVillageName().toLowerCase().contains(name.trim().toLowerCase())
                        || (v.getHeadmanName() != null && v.getHeadmanName().toLowerCase().contains(name.trim().toLowerCase()))
                        || (v.getRegion() != null && v.getRegion().toLowerCase().contains(name.trim().toLowerCase()))
                        || (v.getTraditionalAuthority() != null &&
                            v.getTraditionalAuthority().getAuthorityName().toLowerCase().contains(name.trim().toLowerCase())))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deactivate(Long id) {
        var village = villageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Village", id));
        village.setActive(false);
        villageRepository.save(village);
        log.info("Deactivated Village: {}", village.getVillageName());
    }

    @Override
    public void activate(Long id) {
        var village = villageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Village", id));
        village.setActive(true);
        villageRepository.save(village);
        log.info("Activated Village: {}", village.getVillageName());
    }

    private VillageResponse toResponse(Village v) {
        return VillageResponse.builder()
                .id(v.getId())
                .villageName(v.getVillageName())
                .region(v.getRegion())
                .headmanName(v.getHeadmanName())
                .description(v.getDescription())
                .active(v.getActive())
                .traditionalAuthorityId(
                        v.getTraditionalAuthority().getId())
                .authorityName(
                        v.getTraditionalAuthority().getAuthorityName())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}