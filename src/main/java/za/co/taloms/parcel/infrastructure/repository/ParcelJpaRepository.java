package za.co.taloms.parcel.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.taloms.parcel.domain.entity.Parcel;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.parcel.domain.entity.ParcelType;
import java.util.List;
import java.util.Optional;

public interface ParcelJpaRepository extends JpaRepository<Parcel, Long> {

    Optional<Parcel> findByParcelNumber(String parcelNumber);

    Optional<Parcel> findByStandNumberAndVillageId(String standNumber, Long villageId);

    boolean existsByStandNumberAndVillageId(String standNumber, Long villageId);

    boolean existsByParcelNumber(String parcelNumber);

    List<Parcel> findByVillageId(Long villageId);

    List<Parcel> findByStatus(ParcelStatus status);

    List<Parcel> findByParcelType(ParcelType parcelType);

    @Query("SELECT p FROM Parcel p WHERE p.status = :status AND p.village.id = :villageId")
    List<Parcel> findByStatusAndVillageId(@Param("status") ParcelStatus status,
                                          @Param("villageId") Long villageId);

    @Query("SELECT p FROM Parcel p WHERE p.status = 'AVAILABLE' AND p.village.id = :villageId")
    List<Parcel> findAvailableByVillageId(@Param("villageId") Long villageId);

    long countByStatus(ParcelStatus status);

    long countByVillageId(Long villageId);

    @Query("SELECT p FROM Parcel p ORDER BY p.createdAt DESC")
    List<Parcel> findAllOrderByCreatedAtDesc();
}