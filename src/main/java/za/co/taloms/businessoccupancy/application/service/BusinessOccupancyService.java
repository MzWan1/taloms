package za.co.taloms.businessoccupancy.application.service;

import za.co.taloms.businessoccupancy.application.dto.BusinessOccupancyRequest;
import za.co.taloms.businessoccupancy.application.dto.BusinessOccupancyResponse;
import za.co.taloms.businessoccupancy.domain.entity.BusinessStatus;
import za.co.taloms.businessoccupancy.domain.entity.BusinessType;
import java.util.List;

public interface BusinessOccupancyService {
    BusinessOccupancyResponse createOccupancy(BusinessOccupancyRequest request, String createdBy);
    BusinessOccupancyResponse updateOccupancy(Long id, BusinessOccupancyRequest request, String updatedBy);
    BusinessOccupancyResponse findById(Long id);
    BusinessOccupancyResponse findByRegistrationNumber(String registrationNumber);
    List<BusinessOccupancyResponse> findAll();
    List<BusinessOccupancyResponse> findByParcelId(Long parcelId);
    List<BusinessOccupancyResponse> findByPtoId(Long ptoId);
    List<BusinessOccupancyResponse> findByStatus(BusinessStatus status);
    List<BusinessOccupancyResponse> findByBusinessType(BusinessType businessType);
    List<BusinessOccupancyResponse> searchByName(String name);
    List<BusinessOccupancyResponse> searchByOwnerName(String ownerName);
    BusinessOccupancyResponse updateStatus(Long id, BusinessStatus status, String updatedBy);
    BusinessOccupancyResponse activateOccupancy(Long id, String activatedBy);
    BusinessOccupancyResponse deactivateOccupancy(Long id, String deactivatedBy);
    long countByStatus(BusinessStatus status);
    long countAll();
    boolean existsByParcelId(Long parcelId);
}