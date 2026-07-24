package za.co.taloms.resident.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.DuplicateRecordException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.household.domain.repository.HouseholdRepositoryPort;
import za.co.taloms.resident.application.dto.ResidentRequest;
import za.co.taloms.resident.application.dto.ResidentResponse;
import za.co.taloms.resident.domain.entity.Gender;
import za.co.taloms.resident.domain.entity.RelationshipType;
import za.co.taloms.resident.domain.entity.Resident;
import za.co.taloms.resident.domain.repository.ResidentRepositoryPort;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ResidentServiceImpl implements ResidentService {

    private final ResidentRepositoryPort residentRepository;
    private final HouseholdRepositoryPort householdRepository;

    @Override
    public ResidentResponse createResident(ResidentRequest request, String createdBy) {
        // Validate ID number is unique
        if (residentRepository.existsByIdNumber(request.getIdNumber())) {
            throw new DuplicateRecordException(
                    "A resident with ID number " + request.getIdNumber() + " already exists.");
        }

        // Validate household exists
        var household = householdRepository.findById(request.getHouseholdId())
                .orElseThrow(() -> new ResourceNotFoundException("Household", request.getHouseholdId()));

        // Validate date of birth
        if (request.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new BusinessValidationException("Date of birth cannot be in the future.");
        }

        // Create resident
        var resident = Resident.builder()
                .fullName(request.getFullName())
                .idNumber(request.getIdNumber())
                .dateOfBirth(request.getDateOfBirth())
                .gender(Gender.valueOf(request.getGender()))
                .relationshipType(RelationshipType.valueOf(request.getRelationshipType()))
                .occupation(request.getOccupation())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .household(household)
                .active(true)
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        var saved = residentRepository.save(resident);
        log.info("Resident {} created in household {} by {}",
                saved.getFullName(), request.getHouseholdId(), createdBy);

        return toResponse(saved);
    }

    @Override
    public ResidentResponse updateResident(Long id, ResidentRequest request, String updatedBy) {
        var resident = residentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", id));

        // Check if ID number is being changed and if it's already used
        if (!resident.getIdNumber().equals(request.getIdNumber())
                && residentRepository.existsByIdNumber(request.getIdNumber())) {
            throw new DuplicateRecordException(
                    "ID number " + request.getIdNumber() + " is already used by another resident.");
        }

        var household = householdRepository.findById(request.getHouseholdId())
                .orElseThrow(() -> new ResourceNotFoundException("Household", request.getHouseholdId()));

        resident.setFullName(request.getFullName());
        resident.setIdNumber(request.getIdNumber());
        resident.setDateOfBirth(request.getDateOfBirth());
        resident.setGender(Gender.valueOf(request.getGender()));
        resident.setRelationshipType(RelationshipType.valueOf(request.getRelationshipType()));
        resident.setOccupation(request.getOccupation());
        resident.setContactPhone(request.getContactPhone());
        resident.setContactEmail(request.getContactEmail());
        resident.setHousehold(household);
        resident.setNotes(request.getNotes());

        var saved = residentRepository.save(resident);
        log.info("Resident {} updated by {}", saved.getFullName(), updatedBy);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ResidentResponse findById(Long id) {
        return residentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", id));
    }

    @Override
    @Transactional(readOnly = true)
    public ResidentResponse findByIdNumber(String idNumber) {
        return residentRepository.findByIdNumber(idNumber)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Resident with ID: " + idNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResidentResponse> findAll() {
        return residentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResidentResponse> findByHouseholdId(Long householdId) {
        return residentRepository.findByHouseholdId(householdId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResidentResponse> findActive() {
        return residentRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResidentResponse> searchByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return findAll();
        }
        return residentRepository.findByFullNameContaining(name.trim()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResidentResponse> findByRelationshipType(RelationshipType relationshipType) {
        return residentRepository.findByRelationshipType(relationshipType).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ResidentResponse deactivateResident(Long id, String deactivatedBy) {
        var resident = residentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", id));

        // Don't allow deactivating the household head if it's the only active resident
        if (resident.getRelationshipType() == RelationshipType.HOUSEHOLD_HEAD) {
            var activeResidents = residentRepository.findByHouseholdId(resident.getHousehold().getId())
                    .stream().filter(Resident::isActive).count();
            if (activeResidents <= 1) {
                throw new BusinessValidationException(
                        "Cannot deactivate the household head. The household must have at least one active head.");
            }
        }

        resident.setActive(false);
        var saved = residentRepository.save(resident);
        log.info("Resident {} deactivated by {}", saved.getFullName(), deactivatedBy);

        return toResponse(saved);
    }

    @Override
    public ResidentResponse activateResident(Long id, String activatedBy) {
        var resident = residentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", id));

        resident.setActive(true);
        var saved = residentRepository.save(resident);
        log.info("Resident {} activated by {}", saved.getFullName(), activatedBy);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return residentRepository.countAll();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActive() {
        return residentRepository.countByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByHousehold(Long householdId) {
        return residentRepository.countByHouseholdId(householdId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByIdNumber(String idNumber) {
        return residentRepository.existsByIdNumber(idNumber);
    }

    @Override
    public long countByGender(za.co.taloms.resident.domain.entity.Gender gender) {
        return residentRepository.countByGender(gender);
    }

    private ResidentResponse toResponse(Resident resident) {
        return ResidentResponse.builder()
                .id(resident.getId())
                .fullName(resident.getFullName())
                .idNumber(resident.getIdNumber())
                .dateOfBirth(resident.getDateOfBirth())
                .age(resident.getAge())
                .gender(resident.getGender())
                .genderDisplay(resident.getGender() != null ? resident.getGender().getDisplayName() : null)
                .relationshipType(resident.getRelationshipType())
                .relationshipDisplay(resident.getRelationshipType() != null ? resident.getRelationshipType().getDisplayName() : null)
                .relationshipBadgeClass(resident.getRelationshipType() != null ? resident.getRelationshipType().getBadgeClass() : null)
                .occupation(resident.getOccupation())
                .contactPhone(resident.getContactPhone())
                .contactEmail(resident.getContactEmail())
                .householdId(resident.getHousehold() != null ? resident.getHousehold().getId() : null)
                .householdHeadName(resident.getHousehold() != null ?
                        resident.getHousehold().getHouseholdHeadName() : null)
                .standNumber(resident.getHousehold() != null && resident.getHousehold().getParcel() != null ?
                        resident.getHousehold().getParcel().getStandNumber() : null)
                .villageName(resident.getHousehold() != null && resident.getHousehold().getParcel() != null &&
                        resident.getHousehold().getParcel().getVillage() != null ?
                        resident.getHousehold().getParcel().getVillage().getVillageName() : null)
                .authorityName(resident.getHousehold() != null && resident.getHousehold().getParcel() != null &&
                        resident.getHousehold().getParcel().getVillage() != null &&
                        resident.getHousehold().getParcel().getVillage().getTraditionalAuthority() != null ?
                        resident.getHousehold().getParcel().getVillage().getTraditionalAuthority().getAuthorityName() : null)
                .active(resident.getActive())
                .statusDisplay(resident.isActive() ? "Active" : "Inactive")
                .notes(resident.getNotes())
                .createdBy(resident.getCreatedBy())
                .createdAt(resident.getCreatedAt())
                .updatedAt(resident.getUpdatedAt())
                .build();
    }
}