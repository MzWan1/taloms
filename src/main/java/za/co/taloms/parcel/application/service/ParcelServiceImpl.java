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
    private static final double MIN_AREA_M2 = 1.0;
    private static final double MIN_DISTANCE_BETWEEN_POINTS_M = 0.5;

    @Override
    public ParcelResponse createParcel(ParcelRequest request, String createdBy) {
        if (request.getVillageId() == null) {
            throw new BusinessValidationException("Village is required");
        }

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

        // Keep points in the order they were captured (consecutive order)
        List<BoundaryPointDto> orderedBoundaries = request.getBoundaries().stream()
                .sorted(Comparator.comparingInt(BoundaryPointDto::getSequence))
                .collect(Collectors.toList());

        log.info("Creating parcel with {} raw boundary points", orderedBoundaries.size());

        // Remove duplicate consecutive points first
        List<BoundaryPointDto> uniquePoints = removeDuplicateConsecutivePoints(orderedBoundaries);

        log.info("After removing duplicates: {} unique points", uniquePoints.size());

        // Validate we have enough unique points
        if (uniquePoints.size() < 3) {
            throw new BusinessValidationException(
                    "After removing duplicate points, only " + uniquePoints.size() +
                            " unique points remain. Please capture at least 3 distinct corners.");
        }

        // Log the unique points
        for (int i = 0; i < uniquePoints.size(); i++) {
            BoundaryPointDto p = uniquePoints.get(i);
            log.info("  Point {}: ({}, {})", i + 1, p.getLatitude(), p.getLongitude());
        }

        // Douglas-Peucker simplification to reduce noise while preserving shape
        List<BoundaryPointDto> simplified = BoundarySimplifier.simplify(uniquePoints, 0.5);

        log.info("After simplification: {} points", simplified.size());

        // Ensure we have at least 3 unique points after simplification
        List<BoundaryPointDto> uniqueSimplified = removeDuplicateConsecutivePoints(simplified);

        if (uniqueSimplified.size() < 3) {
            throw new BusinessValidationException(
                    "After simplification, only " + uniqueSimplified.size() +
                            " unique points remain. Please capture more distinct corners.");
        }

        // Close the boundary loop by adding the first point at the end
        List<BoundaryPointDto> closedBoundary = ensureBoundaryIsClosed(uniqueSimplified);

        log.info("After closing: {} points", closedBoundary.size());

        // Validate spatial integrity (coordinates in SA, etc.)
        boundaryValidationService.validateCoordinatesInSouthAfrica(closedBoundary);

        // Calculate area, centroid, and perimeter using the closed boundary
        Double areaM2 = areaCalculator.calculateAreaM2(closedBoundary);

        // Validate area is meaningful
        if (areaM2 < MIN_AREA_M2) {
            throw new BusinessValidationException(
                    "The captured boundary area (" + String.format("%.2f", areaM2) +
                            " m²) is too small. This likely means the points are too close together. " +
                            "Please walk a larger perimeter and capture distinct corners.");
        }

        Double areaHectares = areaCalculator.calculateAreaHectares(closedBoundary);
        Double[] centroid = areaCalculator.calculateCentroid(closedBoundary);
        Double perimeterM = areaCalculator.calculatePerimeterM(closedBoundary);

        log.info("Calculated area: {} m², perimeter: {} m", areaM2, perimeterM);

        // Create parcel entity
        var parcel = Parcel.builder()
                .parcelNumber(generateParcelNumber())
                .standNumber(request.getStandNumber())
                .parcelType(request.getParcelType())
                .status(ParcelStatus.AVAILABLE)
                .areaM2(areaM2)
                .areaHectares(areaHectares)
                .centroidLat(centroid[0])
                .centroidLng(centroid[1])
                .perimeterM(perimeterM)
                .captureMode(request.getCaptureMode() != null ? request.getCaptureMode() : CaptureMode.MANUAL_TAP)
                .village(village)
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        // Save parcel
        var saved = parcelRepository.save(parcel);
        entityManager.flush();

        // Save boundaries in the correct order with proper sequence
        List<ParcelBoundary> boundaries = new ArrayList<>();
        for (int i = 0; i < closedBoundary.size(); i++) {
            BoundaryPointDto point = closedBoundary.get(i);
            var boundary = ParcelBoundary.builder()
                    .parcel(saved)
                    .sequence(i + 1)
                    .latitude(point.getLatitude())
                    .longitude(point.getLongitude())
                    .build();
            boundaries.add(boundary);
        }

        // Save all boundaries
        boundaryRepository.saveAll(boundaries);
        saved.setBoundaries(boundaries);
        entityManager.flush();

        log.info("Parcel created: {} with {} boundary points ({} unique + 1 closure)",
                saved.getParcelNumber(), boundaries.size(), boundaries.size() - 1);

        return toResponse(saved);
    }

    /**
     * Removes consecutive duplicate points (same latitude and longitude).
     */
    private List<BoundaryPointDto> removeDuplicateConsecutivePoints(List<BoundaryPointDto> points) {
        if (points == null || points.isEmpty()) {
            return points;
        }

        List<BoundaryPointDto> result = new ArrayList<>();

        for (BoundaryPointDto point : points) {
            if (point == null || point.getLatitude() == null || point.getLongitude() == null) {
                continue;
            }

            if (result.isEmpty()) {
                result.add(point);
            } else {
                BoundaryPointDto last = result.get(result.size() - 1);
                double latDiff = Math.abs(point.getLatitude() - last.getLatitude());
                double lngDiff = Math.abs(point.getLongitude() - last.getLongitude());

                // Only keep points that are at least 0.000001 degrees apart (~0.1m)
                if (latDiff > 0.000001 || lngDiff > 0.000001) {
                    result.add(point);
                }
            }
        }

        return result;
    }

    /**
     * Ensures the boundary is a closed loop.
     */
    private List<BoundaryPointDto> ensureBoundaryIsClosed(List<BoundaryPointDto> boundaries) {
        if (boundaries == null || boundaries.isEmpty()) {
            return boundaries;
        }

        // Remove any null or invalid points
        List<BoundaryPointDto> validPoints = boundaries.stream()
                .filter(p -> p != null && p.getLatitude() != null && p.getLongitude() != null)
                .collect(Collectors.toList());

        if (validPoints.size() < 3) {
            throw new BusinessValidationException("Need at least 3 valid boundary points");
        }

        // Check if the boundary is already closed
        BoundaryPointDto first = validPoints.get(0);
        BoundaryPointDto last = validPoints.get(validPoints.size() - 1);

        double distance = BoundarySimplifier.haversineDistanceM(
                first.getLatitude(), first.getLongitude(),
                last.getLatitude(), last.getLongitude()
        );

        log.info("Distance between first and last point: {}m", distance);

        // If the last point is not the same as the first (within 1m), add the first point at the end
        if (distance > 1.0) {
            log.info("Boundary not closed. Adding closure point.");

            BoundaryPointDto closurePoint = new BoundaryPointDto();
            closurePoint.setLatitude(first.getLatitude());
            closurePoint.setLongitude(first.getLongitude());
            closurePoint.setSequence(validPoints.size() + 1);
            closurePoint.setAccuracy(first.getAccuracy());
            closurePoint.setAutoCaptured(first.getAutoCaptured());

            List<BoundaryPointDto> result = new ArrayList<>(validPoints);
            result.add(closurePoint);

            log.info("Added closure point. Total points: {}", result.size());
            return result;
        } else if (distance > 0.01) {
            log.info("Snapping last point to first point (distance: {}m)", distance);
            last.setLatitude(first.getLatitude());
            last.setLongitude(first.getLongitude());
        }

        return validPoints;
    }

    @Override
    public ParcelResponse updateParcel(Long id, ParcelRequest request, String updatedBy) {
        var parcel = parcelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parcel", id));

        if (parcel.isAllocated() || parcel.isDisputed()) {
            throw new BusinessValidationException(
                    "Cannot modify parcel with status: " + parcel.getStatus().getDisplayName());
        }

        if (!parcel.getStandNumber().equals(request.getStandNumber()) &&
                parcelRepository.existsByStandNumberAndVillageId(request.getStandNumber(), request.getVillageId())) {
            throw new DuplicateRecordException(
                    "Stand number '" + request.getStandNumber() +
                            "' already exists in this village");
        }

        var village = villageRepository.findById(request.getVillageId())
                .orElseThrow(() -> new ResourceNotFoundException("Village", request.getVillageId()));

        List<BoundaryPointDto> orderedBoundaries = request.getBoundaries().stream()
                .sorted(Comparator.comparingInt(BoundaryPointDto::getSequence))
                .collect(Collectors.toList());

        List<BoundaryPointDto> uniquePoints = removeDuplicateConsecutivePoints(orderedBoundaries);

        if (uniquePoints.size() < 3) {
            throw new BusinessValidationException(
                    "After removing duplicates, only " + uniquePoints.size() +
                            " unique points remain. Please capture at least 3 distinct corners.");
        }

        List<BoundaryPointDto> simplified = BoundarySimplifier.simplify(uniquePoints, 0.5);

        List<BoundaryPointDto> uniqueSimplified = removeDuplicateConsecutivePoints(simplified);

        if (uniqueSimplified.size() < 3) {
            throw new BusinessValidationException(
                    "After simplification, only " + uniqueSimplified.size() +
                            " unique points remain. Please capture more distinct corners.");
        }

        List<BoundaryPointDto> closedBoundary = ensureBoundaryIsClosed(uniqueSimplified);

        boundaryValidationService.validateCoordinatesInSouthAfrica(closedBoundary);

        Double areaM2 = areaCalculator.calculateAreaM2(closedBoundary);

        if (areaM2 < MIN_AREA_M2) {
            throw new BusinessValidationException(
                    "The captured boundary area (" + String.format("%.2f", areaM2) +
                            " m²) is too small. Please capture distinct corners.");
        }

        Double areaHectares = areaCalculator.calculateAreaHectares(closedBoundary);
        Double[] centroid = areaCalculator.calculateCentroid(closedBoundary);
        Double perimeterM = areaCalculator.calculatePerimeterM(closedBoundary);

        boundaryRepository.deleteByParcelId(parcel.getId());

        List<ParcelBoundary> boundaries = new ArrayList<>();
        for (int i = 0; i < closedBoundary.size(); i++) {
            BoundaryPointDto point = closedBoundary.get(i);
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
        parcel.setPerimeterM(perimeterM);
        parcel.setCaptureMode(request.getCaptureMode() != null ? request.getCaptureMode() : CaptureMode.MANUAL_TAP);
        parcel.setVillage(village);
        parcel.setNotes(request.getNotes());
        parcel.setBoundaries(boundaries);

        var saved = parcelRepository.save(parcel);
        entityManager.flush();

        log.info("Parcel updated: {}", saved.getParcelNumber());

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
        List<Parcel> availableParcels = parcelRepository.findAvailable(villageId);

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
    @Transactional(readOnly = true)
    public List<ParcelResponse> findAllAvailable() {
        List<Parcel> availableParcels = parcelRepository.findByStatus(ParcelStatus.AVAILABLE);

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

        log.info("Parcel status updated: {}", status.name());

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

        log.info("Parcel allocated");

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

        log.info("Parcel deleted");
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
        if (boundaries == null || boundaries.size() < 3) {
            return 0.0;
        }
        try {
            List<BoundaryPointDto> uniquePoints = removeDuplicateConsecutivePoints(boundaries);
            if (uniquePoints.size() < 3) {
                return 0.0;
            }
            List<BoundaryPointDto> closedBoundary = ensureBoundaryIsClosed(uniquePoints);
            return areaCalculator.calculateAreaM2(closedBoundary);
        } catch (Exception e) {
            log.warn("Error calculating area: {}", e.getMessage());
            return 0.0;
        }
    }

    private String generateParcelNumber() {
        String year = String.valueOf(java.time.Year.now().getValue());
        long count = parcelRepository.countAll() + 1;
        return String.format("%s-%s-%05d", PARCEL_NUMBER_PREFIX, year, count);
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
                .perimeterM(parcel.getPerimeterM())
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