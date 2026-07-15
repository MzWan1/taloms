package za.co.taloms.household.application.service;

import za.co.taloms.household.application.dto.HouseholdRequest;
import za.co.taloms.household.application.dto.HouseholdResponse;
import java.util.List;

public interface HouseholdService {
    HouseholdResponse createHousehold(HouseholdRequest request, String createdBy);
    HouseholdResponse updateHousehold(Long id, HouseholdRequest request, String updatedBy);
    HouseholdResponse findById(Long id);
    HouseholdResponse findActiveByParcelId(Long parcelId);
    List<HouseholdResponse> findAll();
    List<HouseholdResponse> findByParcelId(Long parcelId);
    List<HouseholdResponse> findByPtoId(Long ptoId);
    List<HouseholdResponse> findActive();
    List<HouseholdResponse> searchByName(String name);
    HouseholdResponse deactivateHousehold(Long id, String deactivatedBy);
    HouseholdResponse activateHousehold(Long id, String activatedBy);
    long countAll();
    long countActive();
    long countByParcel(Long parcelId);
    boolean hasActiveHousehold(Long parcelId);
}