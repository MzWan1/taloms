package za.co.taloms.traditionalauthority.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.DuplicateRecordException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.traditionalauthority.application.dto.*;
import za.co.taloms.traditionalauthority.domain.entity.Village;
import za.co.taloms.traditionalauthority.domain.repository.*;
import za.co.taloms.security.domain.entity.User;
import za.co.taloms.security.domain.repository.UserRepositoryPort;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VillageServiceImpl implements VillageService {

    private final VillageRepositoryPort              villageRepository;
    private final TraditionalAuthorityRepositoryPort authorityRepository;
    private final UserRepositoryPort                 userRepository;
    private final SpatialBoundaryService             spatialBoundaryService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    @CacheEvict(value = {"villages", "villagesByAuthority", "villagesByHeadman"}, allEntries = true)
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

        // Spatial rules: village must be mapped, sit fully inside its own
        // authority's boundary and not overlap any other village/authority.
        // This is what stops a village being placed in another province
        // (e.g. Limpopo) when its authority is in Gauteng.
        var boundaryPoints = spatialBoundaryService.parseBoundary(
                request.getBoundaryJson());
        spatialBoundaryService.validateVillageBoundary(
                null, authority, request.getBoundaryJson(), null);

        // A headsman belongs to exactly ONE authority — validate before saving.
        // Assignment fixes the headsman's authority to this village's authority.
        if (request.getHeadmanId() != null) {
            validateAndLinkHeadman(request.getHeadmanId(),
                    request.getTraditionalAuthorityId(), null);
        }

        String headmanName = request.getHeadmanId() != null ?
                resolveUserName(request.getHeadmanId()) : null;

        var village = Village.builder()
                .villageName(request.getVillageName())
                .region(request.getRegion())
                .headmanName(headmanName)
                .headmanId(request.getHeadmanId())
                .description(request.getDescription())
                .boundaryJson(spatialBoundaryService.toJson(boundaryPoints))
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
    @CacheEvict(value = {"villages", "villagesByAuthority", "villagesByHeadman"}, allEntries = true)
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

        Long previousHeadmanId = village.getHeadmanId();

        // Spatial rules also apply on update (boundary may have been redrawn
        // or the village moved to a different authority)
        spatialBoundaryService.validateVillageBoundary(
                village, authority, request.getBoundaryJson(), id);
        village.setBoundaryJson(spatialBoundaryService.toJson(
                spatialBoundaryService.parseBoundary(request.getBoundaryJson())));

        // A headsman belongs to exactly ONE authority — validate before saving.
        // excludeVillageId = this village, so re-saving the same assignment is allowed.
        if (request.getHeadmanId() != null) {
            validateAndLinkHeadman(request.getHeadmanId(),
                    request.getTraditionalAuthorityId(), id);
        }

        String headmanName = request.getHeadmanId() != null ?
                resolveUserName(request.getHeadmanId()) : null;

        village.setVillageName(request.getVillageName());
        village.setRegion(request.getRegion());
        village.setHeadmanName(headmanName);
        village.setHeadmanId(request.getHeadmanId());
        village.setDescription(request.getDescription());
        village.setTraditionalAuthority(authority);

        var saved = villageRepository.save(village);

        // If the headman was replaced or removed, release their authority link
        // when they no longer serve any village or authority.
        if (previousHeadmanId != null
                && !previousHeadmanId.equals(request.getHeadmanId())) {
            releaseHeadmanIfUnassigned(previousHeadmanId);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("villages")
    public VillageResponse findById(Long id) {
        return villageRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Village", id));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("villages")
    public List<VillageResponse> findAll() {
        return villageRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "villagesByAuthority", key = "#authorityId")
    public List<VillageResponse> findByAuthority(Long authorityId) {
        return villageRepository
                .findByTraditionalAuthorityId(authorityId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "villagesByHeadman", key = "#headmanId")
    public List<VillageResponse> findByHeadmanId(Long headmanId) {
        if (headmanId == null) {
            return java.util.Collections.emptyList();
        }
        return villageRepository.findByHeadmanId(headmanId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "villages", key = "'search:' + (#name == null ? '' : #name.trim().toLowerCase())")
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

    /**
     * Validates that a headsman may be assigned to a village in the given
     * authority, then links the headsman to that authority.
     *
     * Business rule: one headsman belongs to exactly ONE Traditional Authority.
     * The authority is fixed upon village assignment — the village's authority
     * is assigned to the headsman user (user.traditionalAuthorityId).
     *
     * @param headmanId        the user being assigned as headman
     * @param authorityId      the village's authority
     * @param excludeVillageId when updating, the village being edited (so
     *                         re-saving its own assignment is allowed); null on create
     */
    private void validateAndLinkHeadman(Long headmanId, Long authorityId,
                                        Long excludeVillageId) {
        var headman = userRepository.findById(headmanId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", headmanId));

        boolean isHeadsman = headman.getRoles().stream()
                .anyMatch(r -> "ROLE_HEADSMAN".equals(r.getName()));
        if (!isHeadsman) {
            throw new BusinessValidationException(
                    "Selected user is not a headsman: " + headman.getFullName());
        }

        // Already heading a village under a DIFFERENT authority?
        boolean headsVillageElsewhere = villageRepository.findByHeadmanId(headmanId).stream()
                .filter(v -> excludeVillageId == null
                        || !excludeVillageId.equals(v.getId()))
                .anyMatch(v -> !authorityId.equals(
                        v.getTraditionalAuthority().getId()));
        if (headsVillageElsewhere) {
            throw new BusinessValidationException(
                    headman.getFullName()
                            + " is assigned to another Traditional Authority through an existing village. "
                            + "Please choose a headsman who belongs to this authority, or remove the other village assignment first.");
        }

        // Their authority link (if any) must already match this authority
        if (headman.getTraditionalAuthorityId() != null
                && !headman.getTraditionalAuthorityId().equals(authorityId)) {
            throw new BusinessValidationException(
                    headman.getFullName()
                            + " is assigned to another Traditional Authority. "
                            + "Please choose a headsman who belongs to this authority, or clear their current authority assignment first.");
        }

        // Take the village's authority and assign it to the headsman
        headman.setTraditionalAuthorityId(authorityId);
        userRepository.save(headman);
        log.info("Linked headsman {} to authority {}",
                headman.getUsername(), authorityId);
    }

    /**
     * Clears a headsman's authority link when they are no longer the headman
     * of any village and are not set as the headman of any authority, so they
     * can be assigned to a new authority later.
     */
    private void releaseHeadmanIfUnassigned(Long headmanId) {
        userRepository.findById(headmanId).ifPresent(headman -> {
            boolean stillHeadsVillage =
                    !villageRepository.findByHeadmanId(headmanId).isEmpty();
            // Exists check instead of loading every authority into memory.
            boolean isAuthorityHeadman = authorityRepository.existsByHeadmanId(headmanId);
            if (!stillHeadsVillage && !isAuthorityHeadman
                    && headman.getTraditionalAuthorityId() != null) {
                headman.setTraditionalAuthorityId(null);
                userRepository.save(headman);
                log.info("Released headsman {} from their authority link "
                        + "(no longer heads any village or authority)",
                        headman.getUsername());
            }
        });
    }

    private String resolveUserName(Long userId) {
        return userRepository.findById(userId)
                .map(User::getFullName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", userId));
    }

    private VillageResponse toResponse(Village v) {
        return VillageResponse.builder()
                .id(v.getId())
                .villageName(v.getVillageName())
                .region(v.getRegion())
                .headmanName(v.getHeadmanName())
                .headmanId(v.getHeadmanId())
                .chiefName(v.getTraditionalAuthority() != null ? v.getTraditionalAuthority().getChiefName() : null)
                .description(v.getDescription())
                .active(v.getActive())
                .traditionalAuthorityId(
                        v.getTraditionalAuthority().getId())
                .authorityName(
                        v.getTraditionalAuthority().getAuthorityName())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .boundary(toBoundaryResponse(v.getBoundaryJson()))
                .build();
    }

    /** Leniently converts stored boundaryJson to response coordinates. */
    private java.util.List<CoordinateDto> toBoundaryResponse(String boundaryJson) {
        if (boundaryJson == null || boundaryJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(boundaryJson,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(java.util.List.class, CoordinateDto.class));
        } catch (Exception e) {
            log.warn("Could not parse stored village boundary: {}", e.getMessage());
            return null;
        }
    }
}

