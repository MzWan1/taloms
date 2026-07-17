package za.co.taloms.parcel.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.DuplicateRecordException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.parcel.application.dto.BoundaryPointDto;
import za.co.taloms.parcel.application.dto.ParcelRequest;
import za.co.taloms.parcel.application.dto.ParcelResponse;
import za.co.taloms.parcel.domain.entity.Parcel;
import za.co.taloms.parcel.domain.entity.ParcelBoundary;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.parcel.domain.entity.ParcelType;
import za.co.taloms.parcel.domain.repository.ParcelBoundaryRepositoryPort;
import za.co.taloms.parcel.domain.repository.ParcelRepositoryPort;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;
import za.co.taloms.traditionalauthority.domain.repository.VillageRepositoryPort;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private static final String PARCEL_NUMBER_PREFIX = "PRC";

    @Override
    @Transactional(readOnly = true)
    public List<ParcelResponse> findAllAvailable() {
        return parcelRepository.findByStatus(ParcelStatus.AVAILABLE).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
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

        // Validate boundary points
        if (request.getBoundaries() == null || request.getBoundaries().size() < 3) {
            throw new BusinessValidationException("A parcel must have at least 3 boundary points");
        }

        // Validate coordinates are within South Africa
        for (BoundaryPointDto point : request.getBoundaries()) {
            validateSouthAfricanCoordinates(point.getLatitude(), point.getLongitude());
        }

        // Generate parcel number
        String parcelNumber = generateParcelNumber();

        // Calculate area and centroid
        Double areaM2 = areaCalculator.calculateAreaM2(request.getBoundaries());
        Double areaHectares = areaCalculator.calculateAreaHectares(request.getBoundaries());
        Double[] centroid = areaCalculator.calculateCentroid(request.getBoundaries());

        // Create parcel entity
        var parcel = Parcel.builder()
                .parcelNumber(parcelNumber)
                .standNumber(request.getStandNumber())
                .parcelType(ParcelType.valueOf(request.getParcelType()))
                .status(ParcelStatus.AVAILABLE)
                .areaM2(areaM2)
                .areaHectares(areaHectares)
                .centroidLat(centroid[0])
                .centroidLng(centroid[1])
                .village(village)
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        // Save parcel
        var saved = parcelRepository.save(parcel);

        // Save boundaries
        List<ParcelBoundary> boundaries = new ArrayList<>();
        for (int i = 0; i < request.getBoundaries().size(); i++) {
            BoundaryPointDto point = request.getBoundaries().get(i);
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

        // Update boundaries
        boundaryRepository.deleteByParcelId(parcel.getId());

        List<ParcelBoundary> boundaries = new ArrayList<>();
        for (int i = 0; i < request.getBoundaries().size(); i++) {
            BoundaryPointDto point = request.getBoundaries().get(i);
            var boundary = ParcelBoundary.builder()
                    .parcel(parcel)
                    .sequence(i + 1)
                    .latitude(point.getLatitude())
                    .longitude(point.getLongitude())
                    .build();
            boundaries.add(boundary);
        }
        boundaryRepository.saveAll(boundaries);

        // Recalculate area and centroid
        Double areaM2 = areaCalculator.calculateAreaM2(request.getBoundaries());
        Double areaHectares = areaCalculator.calculateAreaHectares(request.getBoundaries());
        Double[] centroid = areaCalculator.calculateCentroid(request.getBoundaries());

        parcel.setStandNumber(request.getStandNumber());
        parcel.setParcelType(ParcelType.valueOf(request.getParcelType()));
        parcel.setAreaM2(areaM2);
        parcel.setAreaHectares(areaHectares);
        parcel.setCentroidLat(centroid[0]);
        parcel.setCentroidLng(centroid[1]);
        parcel.setVillage(village);
        parcel.setNotes(request.getNotes());
        parcel.setBoundaries(boundaries);

        var saved = parcelRepository.save(parcel);
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
    public List<ParcelResponse> findByVillage(Long villageId) {
        return parcelRepository.findByVillageId(villageId).stream()
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
    public List<ParcelResponse> findByParcelType(ParcelType parcelType) {
        return parcelRepository.findByParcelType(parcelType).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParcelResponse> findAvailable(Long villageId) {
        return parcelRepository.findAvailable(villageId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ParcelResponse updateStatus(Long id, ParcelStatus status, String updatedBy) {
        var parcel = parcelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parcel", id));

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

    private void validateSouthAfricanCoordinates(double latitude, double longitude) {
        if (latitude < -35 || latitude > -22) {
            throw new BusinessValidationException(
                    "Latitude " + latitude + " is outside South African bounds (-35 to -22)");
        }
        if (longitude < 16 || longitude > 33) {
            throw new BusinessValidationException(
                    "Longitude " + longitude + " is outside South African bounds (16 to 33)");
        }
    }

    private ParcelResponse toResponse(Parcel parcel) {
        // Load boundaries if not already loaded
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
                .parcelType(parcel.getParcelType())
                .parcelTypeDisplay(parcel.getParcelType().getDisplayName())
                .parcelTypeBadgeClass(parcel.getParcelType().getBadgeClass())
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
                .build();
    }
}