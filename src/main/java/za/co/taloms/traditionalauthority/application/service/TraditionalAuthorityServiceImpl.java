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
import za.co.taloms.security.domain.entity.User;
import za.co.taloms.security.domain.repository.UserRepositoryPort;
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
    private final UserRepositoryPort                 userRepository;
    private final SpatialBoundaryService             spatialBoundaryService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

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

        // Map-drawn boundary: must not overlap any other authority
        var boundaryPoints = spatialBoundaryService.parseBoundary(
                request.getBoundaryJson());
        var boundaryProbe = TraditionalAuthority.builder()
                .id(null)
                .build();
        spatialBoundaryService.validateAuthorityBoundary(
                boundaryProbe, request.getBoundaryJson(), null);

        // A headsman belongs to exactly ONE authority — validate before saving
        if (request.getHeadmanId() != null) {
            validateHeadmanForAuthority(request.getHeadmanId(), null, null);
        }

        // Resolve chief name from user
        String chiefName = resolveUserName(request.getChiefId());
        String headmanName = request.getHeadmanId() != null ?
                resolveUserName(request.getHeadmanId()) : null;

        var authority = TraditionalAuthority.builder()
                .authorityName(request.getAuthorityName())
                .chiefName(chiefName)
                .chiefId(request.getChiefId())
                .headmanName(headmanName)
                .headmanId(request.getHeadmanId())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .physicalAddress(request.getPhysicalAddress())
                .region(request.getRegion())
                .boundaryJson(spatialBoundaryService.toJson(boundaryPoints))
                .active(true)
                .createdBy(createdBy)
                .build();

        var saved = authorityRepository.save(authority);

        // Link the chief user to this authority
        linkChiefToAuthority(request.getChiefId(), saved.getId());

        // Assign this authority to the headman user as well
        if (request.getHeadmanId() != null) {
            linkHeadmanToAuthority(request.getHeadmanId(), saved.getId());
        }

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

        Long previousHeadmanId = authority.getHeadmanId();

        // Map-drawn boundary: must not overlap other authorities and must
        // still contain every village already registered under this authority
        spatialBoundaryService.validateAuthorityBoundary(
                authority, request.getBoundaryJson(), id);
        authority.setBoundaryJson(spatialBoundaryService.toJson(
                spatialBoundaryService.parseBoundary(request.getBoundaryJson())));

        // A headsman belongs to exactly ONE authority — validate before saving
        if (request.getHeadmanId() != null) {
            validateHeadmanForAuthority(request.getHeadmanId(), id, id);
        }

        // Resolve chief name from user
        String chiefName = resolveUserName(request.getChiefId());
        String headmanName = request.getHeadmanId() != null ?
                resolveUserName(request.getHeadmanId()) : null;

        authority.setAuthorityName(request.getAuthorityName());
        authority.setChiefName(chiefName);
        authority.setChiefId(request.getChiefId());
        authority.setHeadmanName(headmanName);
        authority.setHeadmanId(request.getHeadmanId());
        authority.setContactPhone(request.getContactPhone());
        authority.setContactEmail(request.getContactEmail());
        authority.setPhysicalAddress(request.getPhysicalAddress());
        authority.setRegion(request.getRegion());

        var saved = authorityRepository.save(authority);

        // Link the chief user to this authority
        linkChiefToAuthority(request.getChiefId(), saved.getId());

        // Assign this authority to the headman user as well
        if (request.getHeadmanId() != null) {
            linkHeadmanToAuthority(request.getHeadmanId(), saved.getId());
        }

        // If the headman was replaced or removed, release their authority
        // link when they no longer serve any village or authority.
        if (previousHeadmanId != null
                && !previousHeadmanId.equals(request.getHeadmanId())) {
            releaseHeadmanIfUnassigned(previousHeadmanId);
        }

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
        List<TraditionalAuthority> all = authorityRepository.findAll();
        log.info("findAll() raw query returned {} authorities", all.size());

        List<TraditionalAuthorityResponse> result = new java.util.ArrayList<>();
        for (TraditionalAuthority a : all) {
            try {
                result.add(toResponse(a));
            } catch (Exception e) {
                log.error("Failed to map authority id={} name='{}': {}",
                        a.getId(), a.getAuthorityName(), e.getMessage(), e);
            }
        }
        log.info("findAll() mapped {} of {} authorities to responses", result.size(), all.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TraditionalAuthorityResponse> findAllActive() {
        List<TraditionalAuthority> active = authorityRepository.findAllActive();
        log.info("findAllActive() raw query returned {} authorities", active.size());

        List<TraditionalAuthorityResponse> result = new java.util.ArrayList<>();
        for (TraditionalAuthority a : active) {
            try {
                result.add(toResponse(a));
            } catch (Exception e) {
                log.error("Failed to map active authority id={} name='{}': {}",
                        a.getId(), a.getAuthorityName(), e.getMessage(), e);
            }
        }
        log.info("findAllActive() mapped {} of {} authorities to responses", result.size(), active.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TraditionalAuthorityResponse> searchByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return findAll();
        }
        List<TraditionalAuthority> filtered = authorityRepository.findAll().stream()
                .filter(a -> a.getAuthorityName().toLowerCase().contains(name.trim().toLowerCase())
                        || (a.getChiefName() != null && a.getChiefName().toLowerCase().contains(name.trim().toLowerCase()))
                        || (a.getRegion() != null && a.getRegion().toLowerCase().contains(name.trim().toLowerCase())))
                .toList();

        List<TraditionalAuthorityResponse> result = new java.util.ArrayList<>();
        for (TraditionalAuthority a : filtered) {
            try {
                result.add(toResponse(a));
            } catch (Exception e) {
                log.error("Failed to map searched authority id={} name='{}': {}",
                        a.getId(), a.getAuthorityName(), e.getMessage(), e);
            }
        }
        return result;
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

    private String resolveUserName(Long userId) {
        return userRepository.findById(userId)
                .map(User::getFullName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", userId));
    }

    /**
     * Links a Chief user to an authority by setting the user's traditionalAuthorityId.
     * This allows the chief to manage villages within that authority.
     */
    private void linkChiefToAuthority(Long chiefId, Long authorityId) {
        userRepository.findById(chiefId).ifPresent(user -> {
            user.setTraditionalAuthorityId(authorityId);
            userRepository.save(user);
            log.info("Linked chief user {} to authority {}", user.getUsername(), authorityId);
        });
    }

    /**
     * Links a Headman user to an authority by setting the user's
     * traditionalAuthorityId, giving them scope over that authority.
     */
    private void linkHeadmanToAuthority(Long headmanId, Long authorityId) {
        userRepository.findById(headmanId).ifPresent(headman -> {
            headman.setTraditionalAuthorityId(authorityId);
            userRepository.save(headman);
            log.info("Linked headsman user {} to authority {}",
                    headman.getUsername(), authorityId);
        });
    }

    /**
     * Validates that a headsman may serve the given authority.
     *
     * Business rule: one headsman belongs to exactly ONE Traditional Authority.
     *
     * @param headmanId          the user being assigned as authority headman
     * @param authorityId        the authority being assigned (null during create,
     *                           before the authority has an id)
     * @param excludeAuthorityId the authority being edited (re-assigning it to
     *                           the same headman is allowed); ignored when authorityId is null
     */
    private void validateHeadmanForAuthority(Long headmanId, Long authorityId,
                                             Long excludeAuthorityId) {
        var headman = userRepository.findById(headmanId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", headmanId));

        boolean isHeadsman = headman.getRoles().stream()
                .anyMatch(r -> "ROLE_HEADSMAN".equals(r.getName()));
        if (!isHeadsman) {
            throw new BusinessValidationException(
                    "Selected user is not a headsman: " + headman.getFullName());
        }

        // Already the headman of a DIFFERENT authority?
        if (authorityRepository.existsByHeadmanIdAndIdNot(headmanId,
                excludeAuthorityId != null ? excludeAuthorityId : -1L)) {
            throw new BusinessValidationException(
                    headman.getFullName()
                            + " is assigned to another Traditional Authority through an existing village. "
                            + "Please choose a headsman who belongs to this authority, or remove the other village assignment first.");
        }

        // Headman of a village under a different authority?
        boolean headsVillageElsewhere = villageRepository.findByHeadmanId(headmanId).stream()
                .anyMatch(v -> authorityId == null
                        || !authorityId.equals(v.getTraditionalAuthority().getId()));
        if (headsVillageElsewhere) {
            throw new BusinessValidationException(
                    headman.getFullName()
                            + " is a headman of a village under a different Traditional Authority. "
                            + "A headsman may only serve one authority.");
        }

        // Their existing authority link must not point elsewhere
        if (headman.getTraditionalAuthorityId() != null) {
            boolean matches = authorityId != null
                    && headman.getTraditionalAuthorityId().equals(authorityId);
            if (!matches) {
                throw new BusinessValidationException(
                        headman.getFullName()
                                + " is assigned to another Traditional Authority. "
                                + "Please choose a headsman who belongs to this authority, or clear their current authority assignment first.");
            }
        }
    }

    /**
     * Clears a headsman's authority link when they no longer head any village
     * and are not the headman of any authority, so they can be assigned to a
     * new authority later.
     */
    private void releaseHeadmanIfUnassigned(Long headmanId) {
        userRepository.findById(headmanId).ifPresent(headman -> {
            boolean stillHeadsVillage =
                    !villageRepository.findByHeadmanId(headmanId).isEmpty();
            boolean isAuthorityHeadman = authorityRepository.findAll().stream()
                    .anyMatch(a -> headmanId.equals(a.getHeadmanId()));
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

    private TraditionalAuthorityResponse toResponse(
            TraditionalAuthority a) {
        long villageCount = villageRepository
                .countByTraditionalAuthorityId(a.getId());
        return TraditionalAuthorityResponse.builder()
                .id(a.getId())
                .authorityName(a.getAuthorityName())
                .chiefName(a.getChiefName())
                .chiefId(a.getChiefId())
                .headmanName(a.getHeadmanName())
                .headmanId(a.getHeadmanId())
                .contactPhone(a.getContactPhone())
                .contactEmail(a.getContactEmail())
                .physicalAddress(a.getPhysicalAddress())
                .region(a.getRegion())
                .active(a.getActive())
                .villageCount(villageCount)
                .createdBy(a.getCreatedBy())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .boundary(toBoundaryResponse(a.getBoundaryJson()))
                .build();
    }

    /** Leniently converts stored boundaryJson to response coordinates. */
    private List<CoordinateDto> toBoundaryResponse(String boundaryJson) {
        if (boundaryJson == null || boundaryJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(boundaryJson,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, CoordinateDto.class));
        } catch (Exception e) {
            log.warn("Could not parse stored authority boundary: {}", e.getMessage());
            return null;
        }
    }
}

