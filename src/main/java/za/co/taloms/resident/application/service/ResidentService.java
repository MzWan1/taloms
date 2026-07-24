package za.co.taloms.resident.application.service;

import za.co.taloms.resident.application.dto.ResidentRequest;
import za.co.taloms.resident.application.dto.ResidentResponse;
import za.co.taloms.resident.domain.entity.RelationshipType;
import java.util.List;

public interface ResidentService {
    ResidentResponse createResident(ResidentRequest request, String createdBy);
    ResidentResponse updateResident(Long id, ResidentRequest request, String updatedBy);
    ResidentResponse findById(Long id);
    ResidentResponse findByIdNumber(String idNumber);
    List<ResidentResponse> findAll();
    List<ResidentResponse> findByHouseholdId(Long householdId);
    List<ResidentResponse> findActive();
    List<ResidentResponse> searchByName(String name);
    List<ResidentResponse> findByRelationshipType(RelationshipType relationshipType);
    ResidentResponse deactivateResident(Long id, String deactivatedBy);
    ResidentResponse activateResident(Long id, String activatedBy);
    long countAll();
    long countActive();
    long countByHousehold(Long householdId);
    long countByGender(String gender);
    boolean existsByIdNumber(String idNumber);
}