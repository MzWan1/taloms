package za.co.taloms.household.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.DuplicateRecordException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.household.application.dto.HouseholdRequest;
import za.co.taloms.household.application.dto.HouseholdResponse;
import za.co.taloms.household.domain.entity.Household;
import za.co.taloms.household.domain.repository.HouseholdRepositoryPort;
import za.co.taloms.parcel.domain.repository.ParcelRepositoryPort;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HouseholdServiceImpl implements HouseholdService {

    private final HouseholdRepositoryPort householdRepository;
    private final ParcelRepositoryPort parcelRepository;
    private final PTORepositoryPort ptoRepository;

    @Override
    public HouseholdResponse createHousehold(HouseholdRequest request, String createdBy) {
        log.info("Creating household - Head: {}, Parcel: {}, PTO: {}",
                request.getHouseholdHeadName(), request.getParcelId(), request.getPtoId());

        // Validate parcel exists
        var parcel = parcelRepository.findById(request.getParcelId())
                .orElseThrow(() -> new ResourceNotFoundException("Parcel", request.getParcelId()));

        // ===== VALIDATION 1: PTO is required =====
        if (request.getPtoId() == null) {
            throw new BusinessValidationException(
                    "PTO is required to register a household.");
        }

        // ===== VALIDATION 2: PTO must exist and be ACTIVE =====
        var pto = ptoRepository.findById(request.getPtoId())
                .orElseThrow(() -> new ResourceNotFoundException("PTO", request.getPtoId()));

        if (pto.getStatus() != PTOStatus.ACTIVE) {
            throw new BusinessValidationException(
                    "Cannot register household: PTO " + pto.getPtoNumber() +
                            " is not ACTIVE. Current status: " + pto.getStatus().getDisplayName());
        }

        // ===== VALIDATION 3: PTO must belong to the same parcel =====
        if (pto.getParcel() == null || !pto.getParcel().getId().equals(parcel.getId())) {
            throw new BusinessValidationException(
                    "PTO " + pto.getPtoNumber() +
                            " does not belong to parcel " + parcel.getParcelNumber() + " (" + parcel.getStandNumber() + ")");
        }

        // Check if parcel already has an active household
        if (householdRepository.existsActiveByParcelId(request.getParcelId())) {
            throw new DuplicateRecordException(
                    "This parcel already has an active household. Deactivate the existing household first.");
        }

        // Create household
        var household = Household.builder()
                .householdHeadName(request.getHouseholdHeadName())
                .householdHeadIdNumber(request.getHouseholdHeadIdNumber())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .parcel(parcel)
                .pto(pto)
                .registrationDate(request.getRegistrationDate() != null ? request.getRegistrationDate() : LocalDate.now())
                .active(true)
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        var saved = householdRepository.save(household);
        log.info("Household created for parcel {} by {}", parcel.getParcelNumber(), createdBy);

        return toResponse(saved);
    }

    @Override
    public HouseholdResponse updateHousehold(Long id, HouseholdRequest request, String updatedBy) {
        log.info("Updating household - ID: {}, Head: {}", id, request.getHouseholdHeadName());

        var household = householdRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Household", id));

        // Validate parcel exists
        var parcel = parcelRepository.findById(request.getParcelId())
                .orElseThrow(() -> new ResourceNotFoundException("Parcel", request.getParcelId()));

        // ===== VALIDATION 1: PTO is required =====
        if (request.getPtoId() == null) {
            throw new BusinessValidationException(
                    "PTO is required for household registration.");
        }

        // ===== VALIDATION 2: PTO must be ACTIVE =====
        var pto = ptoRepository.findById(request.getPtoId())
                .orElseThrow(() -> new ResourceNotFoundException("PTO", request.getPtoId()));

        if (pto.getStatus() != PTOStatus.ACTIVE) {
            throw new BusinessValidationException(
                    "PTO " + pto.getPtoNumber() +
                            " is not ACTIVE. Current status: " + pto.getStatus().getDisplayName());
        }

        // ===== VALIDATION 3: PTO must belong to the same parcel =====
        if (pto.getParcel() == null || !pto.getParcel().getId().equals(parcel.getId())) {
            throw new BusinessValidationException(
                    "PTO " + pto.getPtoNumber() +
                            " does not belong to parcel " + parcel.getParcelNumber() + " (" + parcel.getStandNumber() + ")");
        }

        // Check if another active household exists on this parcel (excluding this one)
        var activeHousehold = householdRepository.findActiveByParcelId(request.getParcelId());
        if (activeHousehold.isPresent() && !activeHousehold.get().getId().equals(id)) {
            throw new DuplicateRecordException(
                    "Another active household already exists on this parcel.");
        }

        household.setHouseholdHeadName(request.getHouseholdHeadName());
        household.setHouseholdHeadIdNumber(request.getHouseholdHeadIdNumber());
        household.setContactPhone(request.getContactPhone());
        household.setContactEmail(request.getContactEmail());
        household.setParcel(parcel);
        household.setPto(pto);
        household.setRegistrationDate(request.getRegistrationDate() != null ? request.getRegistrationDate() : LocalDate.now());
        household.setNotes(request.getNotes());

        var saved = householdRepository.save(household);
        log.info("Household {} updated by {}", saved.getId(), updatedBy);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public HouseholdResponse findById(Long id) {
        return householdRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Household", id));
    }

    @Override
    @Transactional(readOnly = true)
    public HouseholdResponse findActiveByParcelId(Long parcelId) {
        return householdRepository.findActiveByParcelId(parcelId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HouseholdResponse> findAll() {
        return householdRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HouseholdResponse> findByParcelId(Long parcelId) {
        return householdRepository.findByParcelId(parcelId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HouseholdResponse> findByPtoId(Long ptoId) {
        return householdRepository.findByPtoId(ptoId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HouseholdResponse> findActive() {
        return householdRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HouseholdResponse> searchByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return findAll();
        }
        return householdRepository.findByHouseholdHeadNameContaining(name.trim()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public HouseholdResponse deactivateHousehold(Long id, String deactivatedBy) {
        var household = householdRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Household", id));

        household.setActive(false);
        var saved = householdRepository.save(household);
        log.info("Household {} deactivated by {}", saved.getId(), deactivatedBy);

        return toResponse(saved);
    }

    @Override
    public HouseholdResponse activateHousehold(Long id, String activatedBy) {
        var household = householdRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Household", id));

        // Check if parcel already has an active household
        if (householdRepository.existsActiveByParcelId(household.getParcel().getId())) {
            var active = householdRepository.findActiveByParcelId(household.getParcel().getId());
            if (active.isPresent() && !active.get().getId().equals(id)) {
                throw new BusinessValidationException(
                        "Cannot activate: Another household is already active on this parcel.");
            }
        }

        household.setActive(true);
        var saved = householdRepository.save(household);
        log.info("Household {} activated by {}", saved.getId(), activatedBy);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return householdRepository.countAll();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActive() {
        return householdRepository.countByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByParcel(Long parcelId) {
        return householdRepository.countByParcelId(parcelId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveHousehold(Long parcelId) {
        return householdRepository.existsActiveByParcelId(parcelId);
    }

    private HouseholdResponse toResponse(Household household) {
        if (household == null) {
            return null;
        }

        var parcel = household.getParcel();
        var village = parcel != null ? parcel.getVillage() : null;
        var authority = village != null ? village.getTraditionalAuthority() : null;
        var pto = household.getPto();

        return HouseholdResponse.builder()
                .id(household.getId())
                .householdHeadName(household.getHouseholdHeadName())
                .householdHeadIdNumber(household.getHouseholdHeadIdNumber())
                .contactPhone(household.getContactPhone())
                .contactEmail(household.getContactEmail())
                .parcelId(parcel != null ? parcel.getId() : null)
                .standNumber(parcel != null ? parcel.getStandNumber() : null)
                .parcelNumber(parcel != null ? parcel.getParcelNumber() : null)
                .villageName(village != null ? village.getVillageName() : null)
                .authorityName(authority != null ? authority.getAuthorityName() : null)
                .ptoId(pto != null ? pto.getId() : null)
                .ptoNumber(pto != null ? pto.getPtoNumber() : null)
                .ptoHolderName(pto != null ? pto.getPtoHolderName() : null)
                .registrationDate(household.getRegistrationDate())
                .active(household.getActive() != null ? household.getActive() : false)
                .statusDisplay(Boolean.TRUE.equals(household.getActive()) ? "Active" : "Inactive")
                .notes(household.getNotes())
                .createdBy(household.getCreatedBy())
                .createdAt(household.getCreatedAt())
                .updatedAt(household.getUpdatedAt())
                .build();
    }
}