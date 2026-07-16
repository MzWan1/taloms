package za.co.taloms.businessoccupancy.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.businessoccupancy.domain.entity.BusinessOccupancy;
import za.co.taloms.businessoccupancy.domain.entity.BusinessStatus;
import za.co.taloms.businessoccupancy.domain.entity.BusinessType;
import za.co.taloms.businessoccupancy.domain.repository.BusinessOccupancyRepositoryPort;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BusinessOccupancyRepositoryAdapter implements BusinessOccupancyRepositoryPort {

    private final BusinessOccupancyJpaRepository jpaRepository;

    @Override
    public BusinessOccupancy save(BusinessOccupancy occupancy) {
        return jpaRepository.save(occupancy);
    }

    @Override
    public Optional<BusinessOccupancy> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<BusinessOccupancy> findAll() {
        return jpaRepository.findAllOrderByCreatedAtDesc();
    }

    @Override
    public List<BusinessOccupancy> findByParcelId(Long parcelId) {
        return jpaRepository.findByParcelId(parcelId);
    }

    @Override
    public List<BusinessOccupancy> findByPtoId(Long ptoId) {
        return jpaRepository.findByPtoId(ptoId);
    }

    @Override
    public List<BusinessOccupancy> findByStatus(BusinessStatus status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public List<BusinessOccupancy> findByBusinessType(BusinessType businessType) {
        return jpaRepository.findByBusinessType(businessType);
    }

    @Override
    public List<BusinessOccupancy> findByBusinessNameContaining(String name) {
        return jpaRepository.findByBusinessNameContaining(name);
    }

    @Override
    public List<BusinessOccupancy> findByOwnerNameContaining(String ownerName) {
        return jpaRepository.findByOwnerNameContaining(ownerName);
    }

    @Override
    public List<BusinessOccupancy> findByStatusAndBusinessType(BusinessStatus status, BusinessType businessType) {
        return jpaRepository.findByStatusAndBusinessType(status, businessType);
    }

    @Override
    public boolean existsByParcelId(Long parcelId) {
        return jpaRepository.existsByParcelId(parcelId);
    }

    @Override
    public boolean existsByRegistrationNumber(String registrationNumber) {
        return jpaRepository.existsByRegistrationNumber(registrationNumber);
    }

    @Override
    public Optional<BusinessOccupancy> findByRegistrationNumber(String registrationNumber) {
        return jpaRepository.findByRegistrationNumber(registrationNumber);
    }

    @Override
    public long countByStatus(BusinessStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }
}