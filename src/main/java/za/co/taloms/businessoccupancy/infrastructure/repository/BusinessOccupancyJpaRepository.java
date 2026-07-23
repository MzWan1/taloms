package za.co.taloms.businessoccupancy.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.taloms.businessoccupancy.domain.entity.BusinessOccupancy;
import za.co.taloms.businessoccupancy.domain.entity.BusinessStatus;
import za.co.taloms.businessoccupancy.domain.entity.BusinessType;
import java.util.List;
import java.util.Optional;

public interface BusinessOccupancyJpaRepository extends JpaRepository<BusinessOccupancy, Long> {

    List<BusinessOccupancy> findByParcelId(Long parcelId);

    List<BusinessOccupancy> findByParcelIdIn(List<Long> parcelIds);

    List<BusinessOccupancy> findByPtoId(Long ptoId);

    List<BusinessOccupancy> findByStatus(BusinessStatus status);

    List<BusinessOccupancy> findByBusinessType(BusinessType businessType);

    @Query("SELECT b FROM BusinessOccupancy b WHERE LOWER(b.businessName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<BusinessOccupancy> findByBusinessNameContaining(@Param("name") String name);

    @Query("SELECT b FROM BusinessOccupancy b WHERE LOWER(b.ownerName) LIKE LOWER(CONCAT('%', :ownerName, '%'))")
    List<BusinessOccupancy> findByOwnerNameContaining(@Param("ownerName") String ownerName);

    List<BusinessOccupancy> findByStatusAndBusinessType(BusinessStatus status, BusinessType businessType);

    boolean existsByParcelId(Long parcelId);

    boolean existsByRegistrationNumber(String registrationNumber);

    Optional<BusinessOccupancy> findByRegistrationNumber(String registrationNumber);

    long countByStatus(BusinessStatus status);

    @Query("SELECT b FROM BusinessOccupancy b ORDER BY b.createdAt DESC")
    List<BusinessOccupancy> findAllOrderByCreatedAtDesc();
}