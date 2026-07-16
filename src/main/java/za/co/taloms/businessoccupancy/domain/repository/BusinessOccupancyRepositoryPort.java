package za.co.taloms.businessoccupancy.domain.repository;

import za.co.taloms.businessoccupancy.domain.entity.BusinessOccupancy;
import za.co.taloms.businessoccupancy.domain.entity.BusinessStatus;
import za.co.taloms.businessoccupancy.domain.entity.BusinessType;
import java.util.List;
import java.util.Optional;

public interface BusinessOccupancyRepositoryPort {
    BusinessOccupancy save(BusinessOccupancy occupancy);
    Optional<BusinessOccupancy> findById(Long id);
    List<BusinessOccupancy> findAll();
    List<BusinessOccupancy> findByParcelId(Long parcelId);
    List<BusinessOccupancy> findByPtoId(Long ptoId);
    List<BusinessOccupancy> findByStatus(BusinessStatus status);
    List<BusinessOccupancy> findByBusinessType(BusinessType businessType);
    List<BusinessOccupancy> findByBusinessNameContaining(String name);
    List<BusinessOccupancy> findByOwnerNameContaining(String ownerName);
    List<BusinessOccupancy> findByStatusAndBusinessType(BusinessStatus status, BusinessType businessType);
    boolean existsByParcelId(Long parcelId);
    boolean existsByRegistrationNumber(String registrationNumber);
    Optional<BusinessOccupancy> findByRegistrationNumber(String registrationNumber);
    long countByStatus(BusinessStatus status);
    long countAll();
}