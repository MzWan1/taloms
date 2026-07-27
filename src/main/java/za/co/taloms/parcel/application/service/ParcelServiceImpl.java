package za.co.taloms.parcel.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.DuplicateRecordException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.parcel.application.dto.BoundaryPointDto;
import za.co.taloms.parcel.application.dto.ParcelRequest;
import za.co.taloms.parcel.application.dto.ParcelResponse;
import za.co.taloms.parcel.application.service.BoundarySimplifier;
import za.co.taloms.parcel.application.service.BoundaryValidationService;
import za.co.taloms.parcel.application.service.ParcelAreaCalculator;
import za.co.taloms.parcel.domain.entity.Parcel;
import za.co.taloms.parcel.domain.entity.CaptureMode;
import za.co.taloms.parcel.domain.entity.ParcelBoundary;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.parcel.domain.repository.ParcelBoundaryRepositoryPort;
import za.co.taloms.parcel.domain.repository.ParcelRepositoryPort;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;
import za.co.taloms.traditionalauthority.domain.repository.VillageRepositoryPort;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParcelServiceImpl implements ParcelService {

    private final ParcelRepositoryPort parcelRepository;
    private final ParcelBoundaryRepositoryPort boundaryRepository;
    private final VillageRepositoryPort villageRepository;
    private final PTORepositoryPort ptoRepository;
    private final ParcelAreaCalculator areaCalculator;
    private final ApplicationEventPublisher eventPublisher;
    private final BoundaryValidationService boundaryValidationService;
    private final EntityManager entityManager;

    private static final String PARCEL_NUMBER_PREFIX = "PRC";

    @Override
    public ParcelResponse createParcel(ParcelRequest request, String createdBy) {
        // Validate village exists
        var village = villageRepository.findById(request.getVillageId())
                .orElseThrow(() -> new ResourceNotFoundException("Village", request.getVillageId()));

        // Validate stand number uniqueness within village
        if (parcelRepository.existsByStandNumberAndVillageId(request.getStandNumber(), request.getVillageId())) {
            throw new DuplicateRecordException(
                    "Stand number '" + request.getStandNumber() +
                            "' already exists in this village");
        }

        // Validate boundary points count
        if (request.getBoundaries() == null || request.getBoundaries().size() < 3) {
            throw new BusinessValidationException("A parcel must have at least 3 boundary points");
        }

        List<BoundaryPointDto> orderedBoundaries = request.getBoundaries().stream()
                .sorted(Comparator.comparingInt(BoundaryPointDto::getSequence))
                .collect(Collectors.toList());

        // Douglas-Peucker simplification — reduces walk-trace noise to meaningful vertices
        List<BoundaryPointDto> simplified = BoundarySimplifier.simplify(orderedBoundaries, 0.5);
        if (simplified.size() < 3) {
            throw new BusinessValidationException(
                    "Boundary too simple after simplification. Please capture more points.");
        }

        // Validate spatial integrity
        boundaryValidationService.validateCoordinatesInSouthAfrica(simplified);
        boundaryValidationService.validateClosedLoop(simplified);
        boundaryValidationService.validateMinimumArea(simplified, null);

        // Calculate area and centroid using UTM Zone 35S projection
        Double areaM2 = areaCalculator.calculateAreaM2(simplified);
        Double areaHectares = areaCalculator.calculateAreaHectares(simplified);
        Double[] centroid = areaCalculator.calculateCentroid(simplified);

        // Create parcel entity
        var parcel = Parcel.builder()
                .parcelNumber(generateParcelNumber())
                .standNumber(request.getStandNumber())
                .status(ParcelStatus.AVAILABLE)
                .areaM2(areaM2)
                .areaHectares(areaHectares)
                .centroidLat(centroid[0])
                .centroidLng(centroid[1])
                .captureMode(request.getCaptureMode() != null ? request.getCaptureMode() : CaptureMode.MANUAL_TAP)
                .village(village)
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        // Save parcel
        var saved = parcelRepository.save(parcel);
        entityManager.flush();

        // Save simplified boundaries
        List<ParcelBoundary> boundaries = new ArrayList<>();
        for (int i = 0; i < simplified.size(); i++) {
            BoundaryPointDto point = simplified.get(i);
            var boundary = ParcelBoundary.builder()
                    .parcel(saved)
                    .sequence(i + 1)
                    .latitude(point.getLatitude())
                    .longitude(point.getLongitude())
                    .build();
            boundaries.add(boundary);
        }
        boundaryRepository.saveAll(boundaries);
        saved.setBoundaries(boundaries);
        entityManager.flush();

        // PostGIS geometry is auto-updated via V30 trigger on parcel_boundaries

        // Check for self-intersecting polygon
        if (parcelRepository.hasSelfIntersection(saved.getId())) {
            log.warn("Parcel {} has self-intersecting geometry — ST_IsValid returned false", saved.getId());
        }

        log.info("Created parcel: {} ({}) in village: {} by {}",
                saved.getParcelNumber(), saved.getStandNumber(), village.getVillageName(), createdBy);

        return toResponse(saved);
    }

    @Override
    public ParcelResponse updateParcel(Long id, ParcelRequest request, String updatedBy) {
        var parcel = parcelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parcel", id));

        // Cannot modify allocated or disputed parcels
        if (parcel.isAllocated() || parcel.isDisputed()) {
            throw new BusinessValidationException(
                    "Cannot modify parcel with status: " + parcel.getStatus().getDisplayName());
        }

        // Validate stand number uniqueness if changed
        if (!parcel.getStandNumber().equals(request.getStandNumber()) &&
                parcelRepository.existsByStandNumberAndVillageId(request.getStandNumber(), request.getVillageId())) {
            throw new DuplicateRecordException(
                    "Stand number '" + request.getStandNumber() +
                            "' already exists in this village");
        }

        var village = villageRepository.findById(request.getVillageId())
                .orElseThrow(() -> new ResourceNotFoundException("Village", request.getVillageId()));

        // Douglas-Peucker simplification
        List<BoundaryPointDto> orderedBoundaries = request.getBoundaries().stream()
                .sorted(Comparator.comparingInt(BoundaryPointDto::getSequence))
                .collect(Collectors.toList());
        List<BoundaryPointDto> simplified = BoundarySimplifier.simplify(orderedBoundaries, 0.5);
        if (simplified.size() < 3) {
            throw new BusinessValidationException(
                    "Boundary too simple after simplification. Please capture more points.");
        }

        // Validate spatial integrity
        boundaryValidationService.validateCoordinatesInSouthAfrica(simplified);
        boundaryValidationService.validateClosedLoop(simplified);

        // Recalculate area and centroid using UTM Zone 35S
        Double areaM2 = areaCalculator.calculateAreaM2(simplified);
        Double areaHectares = areaCalculator.calculateAreaHectares(simplified);
        Double[] centroid = areaCalculator.calculateCentroid(simplified);

        // Replace boundaries
        boundaryRepository.deleteByParcelId(parcel.getId());

        List<ParcelBoundary> boundaries = new ArrayList<>();
        for (int i = 0; i < simplified.size(); i++) {
            BoundaryPointDto point = simplified.get(i);
            var boundary = ParcelBoundary.builder()
                    .parcel(parcel)
                    .sequence(i + 1)
                    .latitude(point.getLatitude())
                    .longitude(point.getLongitude())
                    .build();
            boundaries.add(boundary);
        }
        boundaryRepository.saveAll(boundaries);

        parcel.setStandNumber(request.getStandNumber());
        parcel.setAreaM2(areaM2);
        parcel.setAreaHectares(areaHectares);
        parcel.setCentroidLat(centroid[0]);
        parcel.setCentroidLng(centroid[1]);
        parcel.setCaptureMode(request.getCaptureMode() != null ? request.getCaptureMode() : CaptureMode.MANUAL_TAP);
        parcel.setVillage(village);
        parcel.setNotes(request.getNotes());
        parcel.setBoundaries(boundaries);

        var saved = parcelRepository.save(parcel);
        entityManager.flush();

        // Check for self-intersecting polygon
        if (parcelRepository.hasSelfIntersection(saved.getId())) {
            log.warn("Parcel {} has self-intersecting geometry after update", saved.getId());
        }

        log.info("Updated parcel: {} by {}", saved.getParcelNumber(), updatedBy);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ParcelResponse findById(Long id) {
        return parcelRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Parcel", id));
    }

    @Override
    @Transactional(readOnly = true)
    public ParcelResponse findByParcelNumber(String parcelNumber) {
        return parcelRepository.findByParcelNumber(parcelNumber)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Parcel not found: " + parcelNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParcelResponse> findAll() {
        return parcelRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParcelResponse> findByStatus(ParcelStatus status) {
        return parcelRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParcelResponse> findByVillage(Long villageId) {
        return parcelRepository.findByVillageId(villageId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParcelResponse> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        String q = query.trim().toLowerCase();
        return parcelRepository.findAll().stream()
                .filter(p -> p.getParcelNumber() != null && p.getParcelNumber().toLowerCase().contains(q))
                .filter(p -> p.getStandNumber() != null && p.getStandNumber().toLowerCase().contains(q))
                .filter(p -> p.getVillage() != null && p.getVillage().getVillageName() != null
                        && p.getVillage().getVillageName().toLowerCase().contains(q))
                .filter(p -> p.getVillage() != null && p.getVillage().getTraditionalAuthority() != null
                        && p.getVillage().getTraditionalAuthority().getAuthorityName().toLowerCase().contains(q))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParcelResponse> findAvailable(Long villageId) {
        // Get all parcels with AVAILABLE status
        List<Parcel> availableParcels = parcelRepository.findAvailable(villageId);

        // Filter out parcels that have ACTIVE or SUSPENDED PTOs
        return availableParcels.stream()
                .filter(parcel -> {
                    // Check if this parcel has an ACTIVE or SUSPENDED PTO
                    boolean hasActivePto = ptoRepository.existsByParcelIdAndStatus(parcel.getId(), PTOStatus.ACTIVE);
                    boolean hasSuspendedPto = ptoRepository.existsByParcelIdAndStatus(parcel.getId(), PTOStatus.SUSPENDED);

                    // Parcel is available only if it has NO ACTIVE and NO SUSPENDED PTO
                    return !hasActivePto && !hasSuspendedPto;
                })
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParcelResponse> findAllAvailable() {
        // Get all parcels with AVAILABLE status
        List<Parcel> availableParcels = parcelRepository.findByStatus(ParcelStatus.AVAILABLE);

        // Filter out parcels that have ACTIVE or SUSPENDED PTOs
        return availableParcels.stream()
                .filter(parcel -> {
                    boolean hasActivePto = ptoRepository.existsByParcelIdAndStatus(parcel.getId(), PTOStatus.ACTIVE);
                    boolean hasSuspendedPto = ptoRepository.existsByParcelIdAndStatus(parcel.getId(), PTOStatus.SUSPENDED);
                    return !hasActivePto && !hasSuspendedPto;
                })
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ParcelResponse updateStatus(Long id, ParcelStatus status, String updatedBy) {
        var parcel = parcelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parcel", id));

        // Check if trying to set to AVAILABLE - verify no ACTIVE PTO exists
        if (status == ParcelStatus.AVAILABLE) {
            boolean hasActivePto = ptoRepository.existsByParcelIdAndStatus(id, PTOStatus.ACTIVE);
            if (hasActivePto) {
                throw new BusinessValidationException(
                        "Cannot set parcel to AVAILABLE because it has an ACTIVE PTO. " +
                                "Revoke the PTO first.");
            }
        }

        parcel.setStatus(status);
        var saved = parcelRepository.save(parcel);

        log.info("Parcel {} status updated to {} by {}",
                saved.getParcelNumber(), status.getDisplayName(), updatedBy);

        return toResponse(saved);
    }

    @Override
    public ParcelResponse allocateParcel(Long id, Long ptoId, String allocatedBy) {
        var parcel = parcelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parcel", id));

        if (!parcel.canBeAllocated()) {
            throw new BusinessValidationException(
                    "Parcel cannot be allocated. Current status: " + parcel.getStatus().getDisplayName());
        }

        var pto = ptoRepository.findById(ptoId)
                .orElseThrow(() -> new ResourceNotFoundException("PTO", ptoId));

        parcel.setStatus(ParcelStatus.ALLOCATED);
        parcel.setPto(pto);
        var saved = parcelRepository.save(parcel);

        log.info("Parcel {} allocated to PTO {} by {}",
                saved.getParcelNumber(), pto.getPtoNumber(), allocatedBy);

        return toResponse(saved);
    }

    @Override
    public void deleteParcel(Long id, String deletedBy) {
        var parcel = parcelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parcel", id));

        if (parcel.isAllocated()) {
            throw new BusinessValidationException("Cannot delete an allocated parcel");
        }

        boundaryRepository.deleteByParcelId(id);
        parcelRepository.deleteById(id);

        log.info("Parcel {} deleted by {}", parcel.getParcelNumber(), deletedBy);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(ParcelStatus status) {
        return parcelRepository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByVillage(Long villageId) {
        return parcelRepository.countByVillageId(villageId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatusAndVillage(ParcelStatus status, Long villageId) {
        return parcelRepository.countByStatusAndVillageId(status, villageId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return parcelRepository.countAll();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStandNumberUnique(String standNumber, Long villageId) {
        return !parcelRepository.existsByStandNumberAndVillageId(standNumber, villageId);
    }

    @Override
    public Double calculateArea(List<BoundaryPointDto> boundaries) {
        return areaCalculator.calculateAreaM2(boundaries);
    }

    private String generateParcelNumber() {
        String year = String.valueOf(java.time.Year.now().getValue());
        long count = parcelRepository.countAll() + 1;
        return String.format("%s-%s-%05d", PARCEL_NUMBER_PREFIX, year, count);
    }

    private void checkForOverlaps(Parcel parcel) {
        if (parcel.getBoundaries() == null || parcel.getBoundaries().isEmpty()) {
            return;
        }

        List<Object[]> overlaps = parcelRepository.findOverlappingParcelsWithGeometry(parcel.getId());

        if (overlaps != null && !overlaps.isEmpty()) {
            String names = overlaps.stream()
                    .map(row -> row[1] + " (" + row[2] + ")")
                    .collect(Collectors.joining(", "));
            throw new BusinessValidationException(
                    "Parcel boundary overlaps with existing parcel(s): " + names +
                    ". Please adjust the boundary points to avoid disputes.");
        }
    }

    private ParcelResponse toResponse(Parcel parcel) {
        List<BoundaryPointDto> boundaryPoints = parcel.getBoundaries().stream()
                .map(b -> BoundaryPointDto.builder()
                        .sequence(b.getSequence())
                        .latitude(b.getLatitude())
                        .longitude(b.getLongitude())
                        .build())
                .collect(Collectors.toList());

        return ParcelResponse.builder()
                .id(parcel.getId())
                .parcelNumber(parcel.getParcelNumber())
                .standNumber(parcel.getStandNumber())
                .status(parcel.getStatus())
                .statusDisplay(parcel.getStatus().getDisplayName())
                .statusBadgeClass(parcel.getStatus().getBadgeClass())
                .areaM2(parcel.getAreaM2())
                .areaHectares(parcel.getAreaHectares())
                .centroidLat(parcel.getCentroidLat())
                .centroidLng(parcel.getCentroidLng())
                .villageId(parcel.getVillage() != null ? parcel.getVillage().getId() : null)
                .villageName(parcel.getVillage() != null ? parcel.getVillage().getVillageName() : null)
                .authorityName(parcel.getVillage() != null && parcel.getVillage().getTraditionalAuthority() != null ?
                        parcel.getVillage().getTraditionalAuthority().getAuthorityName() : null)
                .ptoId(parcel.getPto() != null ? parcel.getPto().getId() : null)
                .ptoNumber(parcel.getPto() != null ? parcel.getPto().getPtoNumber() : null)
                .ptoHolderName(parcel.getPto() != null ? parcel.getPto().getPtoHolderName() : null)
                .notes(parcel.getNotes())
                .createdBy(parcel.getCreatedBy())
                .createdAt(parcel.getCreatedAt())
                .updatedAt(parcel.getUpdatedAt())
                .boundaries(boundaryPoints)
                .boundaryCount(boundaryPoints.size())
                .captureMode(parcel.getCaptureMode())
                .build();
    }
}

