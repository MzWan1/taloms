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

    @Query("""
           SELECT DISTINCT p FROM Parcel p
           LEFT JOIN FETCH p.boundaries b
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority ta
           LEFT JOIN FETCH p.pto
           ORDER BY p.createdAt DESC
           """)
    List<Parcel> findAllOrderByCreatedAtDesc();

    @Query("""
           SELECT DISTINCT p FROM Parcel p
           LEFT JOIN FETCH p.boundaries b
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority ta
           LEFT JOIN FETCH p.pto
           WHERE p.parcelNumber = :parcelNumber
           """)
    Optional<Parcel> findByParcelNumber(@Param("parcelNumber") String parcelNumber);

    @Query("""
           SELECT DISTINCT p FROM Parcel p
           LEFT JOIN FETCH p.boundaries b
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority ta
           LEFT JOIN FETCH p.pto
           WHERE p.village.id = :villageId
           """)
    List<Parcel> findByVillageId(@Param("villageId") Long villageId);

    @Query("""
           SELECT DISTINCT p FROM Parcel p
           LEFT JOIN FETCH p.boundaries b
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority ta
           LEFT JOIN FETCH p.pto
           WHERE p.status = :status
           """)
    List<Parcel> findByStatus(@Param("status") ParcelStatus status);

    @Query("""
           SELECT DISTINCT p FROM Parcel p
           LEFT JOIN FETCH p.boundaries b
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority ta
           LEFT JOIN FETCH p.pto
           WHERE p.parcelType = :parcelType
           """)
    List<Parcel> findByParcelType(@Param("parcelType") ParcelType parcelType);

    @Query("""
           SELECT DISTINCT p FROM Parcel p
           LEFT JOIN FETCH p.boundaries b
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority ta
           LEFT JOIN FETCH p.pto
           WHERE p.status = :status AND p.village.id = :villageId
           """)
    List<Parcel> findByStatusAndVillageId(@Param("status") ParcelStatus status,
                                          @Param("villageId") Long villageId);

    @Query("""
           SELECT DISTINCT p FROM Parcel p
           LEFT JOIN FETCH p.boundaries b
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority ta
           LEFT JOIN FETCH p.pto
           WHERE p.status = 'AVAILABLE' AND p.village.id = :villageId
           """)
    List<Parcel> findAvailableByVillageId(@Param("villageId") Long villageId);

    @Query("""
           SELECT DISTINCT p FROM Parcel p
           LEFT JOIN FETCH p.boundaries b
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority ta
           LEFT JOIN FETCH p.pto
           WHERE p.id = :id
           """)
    Optional<Parcel> findByIdWithRelations(@Param("id") Long id);

    Optional<Parcel> findByStandNumberAndVillageId(String standNumber, Long villageId);

    boolean existsByStandNumberAndVillageId(String standNumber, Long villageId);

    boolean existsByParcelNumber(String parcelNumber);

    long countByStatus(ParcelStatus status);

    long countByVillageId(Long villageId);
}