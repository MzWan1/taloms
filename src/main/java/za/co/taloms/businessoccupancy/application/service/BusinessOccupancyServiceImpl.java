package za.co.taloms.businessoccupancy.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.DuplicateRecordException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.businessoccupancy.application.dto.BusinessOccupancyRequest;
import za.co.taloms.businessoccupancy.application.dto.BusinessOccupancyResponse;
import za.co.taloms.businessoccupancy.domain.entity.BusinessOccupancy;
import za.co.taloms.businessoccupancy.domain.entity.BusinessStatus;
import za.co.taloms.businessoccupancy.domain.entity.BusinessType;
import za.co.taloms.businessoccupancy.domain.repository.BusinessOccupancyRepositoryPort;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.parcel.domain.repository.ParcelRepositoryPort;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BusinessOccupancyServiceImpl implements BusinessOccupancyService {

    private final BusinessOccupancyRepositoryPort businessRepository;
    private final ParcelRepositoryPort parcelRepository;
    private final PTORepositoryPort ptoRepository;

    @Override
    public BusinessOccupancyResponse createOccupancy(BusinessOccupancyRequest request, String createdBy) {
        // Validate parcel exists
        var parcel = parcelRepository.findById(request.getParcelId())
                .orElseThrow(() -> new ResourceNotFoundException("Parcel", request.getParcelId()));

        // Validate PTO exists
        var pto = ptoRepository.findById(request.getPtoId())
                .orElseThrow(() -> new ResourceNotFoundException("PTO", request.getPtoId()));

        // Check if parcel already has a business
        if (businessRepository.existsByParcelId(request.getParcelId())) {
            throw new DuplicateRecordException("This parcel already has a registered business.");
        }

        // Check if registration number is unique (if provided)
        if (request.getRegistrationNumber() != null && !request.getRegistrationNumber().isBlank()) {
            if (businessRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
                throw new DuplicateRecordException(
                        "Registration number " + request.getRegistrationNumber() + " is already in use.");
            }
        }

        // Create business occupancy
        var occupancy = BusinessOccupancy.builder()
                .businessName(request.getBusinessName())
                .registrationNumber(request.getRegistrationNumber())
                .businessType(BusinessType.valueOf(request.getBusinessType()))
                .ownerName(request.getOwnerName())
                .ownerIdNumber(request.getOwnerIdNumber())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .parcel(parcel)
                .pto(pto)
                .operatingHours(request.getOperatingHours())
                .employeesCount(request.getEmployeesCount() != null ? request.getEmployeesCount() : 0)
                .status(BusinessStatus.PENDING)
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        var saved = businessRepository.save(occupancy);
        log.info("Business occupancy created for {} at parcel {} by {}",
                saved.getBusinessName(), parcel.getParcelNumber(), createdBy);

        return toResponse(saved);
    }

    @Override
    public BusinessOccupancyResponse updateOccupancy(Long id, BusinessOccupancyRequest request, String updatedBy) {
        var occupancy = businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business Occupancy", id));

        // Validate parcel exists
        var parcel = parcelRepository.findById(request.getParcelId())
                .orElseThrow(() -> new ResourceNotFoundException("Parcel", request.getParcelId()));

        // Validate PTO exists
        var pto = ptoRepository.findById(request.getPtoId())
                .orElseThrow(() -> new ResourceNotFoundException("PTO", request.getPtoId()));

        // Check if registration number is being changed and if it's already used
        if (request.getRegistrationNumber() != null && !request.getRegistrationNumber().isBlank()) {
            var existing = businessRepository.findByRegistrationNumber(request.getRegistrationNumber());
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new DuplicateRecordException(
                        "Registration number " + request.getRegistrationNumber() + " is already in use.");
            }
        }

        occupancy.setBusinessName(request.getBusinessName());
        occupancy.setRegistrationNumber(request.getRegistrationNumber());
        occupancy.setBusinessType(BusinessType.valueOf(request.getBusinessType()));
        occupancy.setOwnerName(request.getOwnerName());
        occupancy.setOwnerIdNumber(request.getOwnerIdNumber());
        occupancy.setContactPhone(request.getContactPhone());
        occupancy.setContactEmail(request.getContactEmail());
        occupancy.setParcel(parcel);
        occupancy.setPto(pto);
        occupancy.setOperatingHours(request.getOperatingHours());
        occupancy.setEmployeesCount(request.getEmployeesCount() != null ? request.getEmployeesCount() : 0);
        occupancy.setNotes(request.getNotes());

        var saved = businessRepository.save(occupancy);
        log.info("Business occupancy {} updated by {}", saved.getBusinessName(), updatedBy);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessOccupancyResponse findById(Long id) {
        return businessRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Business Occupancy", id));
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessOccupancyResponse findByRegistrationNumber(String registrationNumber) {
        return businessRepository.findByRegistrationNumber(registrationNumber)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Business with registration number: " + registrationNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessOccupancyResponse> findAll() {
        return businessRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessOccupancyResponse> findByParcelId(Long parcelId) {
        return businessRepository.findByParcelId(parcelId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessOccupancyResponse> findByPtoId(Long ptoId) {
        return businessRepository.findByPtoId(ptoId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessOccupancyResponse> findByStatus(BusinessStatus status) {
        return businessRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessOccupancyResponse> findByBusinessType(BusinessType businessType) {
        return businessRepository.findByBusinessType(businessType).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessOccupancyResponse> searchByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return findAll();
        }
        return businessRepository.findByBusinessNameContaining(name.trim()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessOccupancyResponse> searchByOwnerName(String ownerName) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            return findAll();
        }
        return businessRepository.findByOwnerNameContaining(ownerName.trim()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BusinessOccupancyResponse updateStatus(Long id, BusinessStatus status, String updatedBy) {
        var occupancy = businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business Occupancy", id));

        occupancy.setStatus(status);
        var saved = businessRepository.save(occupancy);
        log.info("Business {} status updated to {} by {}",
                saved.getBusinessName(), status.getDisplayName(), updatedBy);

        return toResponse(saved);
    }

    @Override
    public BusinessOccupancyResponse activateOccupancy(Long id, String activatedBy) {
        var occupancy = businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business Occupancy", id));

        occupancy.setStatus(BusinessStatus.ACTIVE);
        var saved = businessRepository.save(occupancy);
        log.info("Business {} activated by {}", saved.getBusinessName(), activatedBy);

        return toResponse(saved);
    }

    @Override
    public BusinessOccupancyResponse deactivateOccupancy(Long id, String deactivatedBy) {
        var occupancy = businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business Occupancy", id));

        occupancy.setStatus(BusinessStatus.INACTIVE);
        var saved = businessRepository.save(occupancy);
        log.info("Business {} deactivated by {}", saved.getBusinessName(), deactivatedBy);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(BusinessStatus status) {
        return businessRepository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return businessRepository.countAll();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByParcelId(Long parcelId) {
        return businessRepository.existsByParcelId(parcelId);
    }

    private BusinessOccupancyResponse toResponse(BusinessOccupancy occupancy) {
        return BusinessOccupancyResponse.builder()
                .id(occupancy.getId())
                .businessName(occupancy.getBusinessName())
                .registrationNumber(occupancy.getRegistrationNumber())
                .businessType(occupancy.getBusinessType())
                .businessTypeDisplay(occupancy.getBusinessType().getDisplayName())
                .businessTypeBadgeClass(occupancy.getBusinessType().getBadgeClass())
                .ownerName(occupancy.getOwnerName())
                .ownerIdNumber(occupancy.getOwnerIdNumber())
                .contactPhone(occupancy.getContactPhone())
                .contactEmail(occupancy.getContactEmail())
                .parcelId(occupancy.getParcel() != null ? occupancy.getParcel().getId() : null)
                .standNumber(occupancy.getParcel() != null ? occupancy.getParcel().getStandNumber() : null)
                .parcelNumber(occupancy.getParcel() != null ? occupancy.getParcel().getParcelNumber() : null)
                .villageName(occupancy.getParcel() != null && occupancy.getParcel().getVillage() != null ?
                        occupancy.getParcel().getVillage().getVillageName() : null)
                .authorityName(occupancy.getParcel() != null && occupancy.getParcel().getVillage() != null &&
                        occupancy.getParcel().getVillage().getTraditionalAuthority() != null ?
                        occupancy.getParcel().getVillage().getTraditionalAuthority().getAuthorityName() : null)
                .ptoId(occupancy.getPto() != null ? occupancy.getPto().getId() : null)
                .ptoNumber(occupancy.getPto() != null ? occupancy.getPto().getPtoNumber() : null)
                .ptoHolderName(occupancy.getPto() != null ? occupancy.getPto().getPtoHolderName() : null)
                .operatingHours(occupancy.getOperatingHours())
                .employeesCount(occupancy.getEmployeesCount())
                .status(occupancy.getStatus())
                .statusDisplay(occupancy.getStatus().getDisplayName())
                .statusBadgeClass(occupancy.getStatus().getBadgeClass())
                .notes(occupancy.getNotes())
                .createdBy(occupancy.getCreatedBy())
                .createdAt(occupancy.getCreatedAt())
                .updatedAt(occupancy.getUpdatedAt())
                .build();
    }
}