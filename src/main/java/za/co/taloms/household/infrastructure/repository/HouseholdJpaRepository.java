package za.co.taloms.household.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.taloms.household.domain.entity.Household;
import java.util.List;
import java.util.Optional;

public interface HouseholdJpaRepository extends JpaRepository<Household, Long> {

    @Query("""
           SELECT h FROM Household h
           LEFT JOIN FETCH h.parcel p
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority
           WHERE h.parcel.id = :parcelId
           """)
    List<Household> findByParcelId(@Param("parcelId") Long parcelId);

    @Query("""
           SELECT h FROM Household h
           LEFT JOIN FETCH h.parcel p
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority
           WHERE h.parcel.id IN :parcelIds
           """)
    List<Household> findByParcelIdIn(@Param("parcelIds") List<Long> parcelIds);

    @Query("""
           SELECT h FROM Household h
           LEFT JOIN FETCH h.parcel p
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority
           LEFT JOIN FETCH h.pto
           WHERE h.pto.id = :ptoId
           """)
    List<Household> findByPtoId(@Param("ptoId") Long ptoId);

    @Query("""
           SELECT h FROM Household h
           LEFT JOIN FETCH h.parcel p
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority
           WHERE h.active = true
           """)
    List<Household> findByActiveTrue();

    @Query("""
           SELECT h FROM Household h
           LEFT JOIN FETCH h.parcel p
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority
           WHERE h.parcel.id = :parcelId AND h.active = true
           """)
    Optional<Household> findActiveByParcelId(@Param("parcelId") Long parcelId);

    @Query("""
           SELECT h FROM Household h
           LEFT JOIN FETCH h.parcel p
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority
           WHERE LOWER(h.householdHeadName) LIKE LOWER(CONCAT('%', :name, '%'))
           """)
    List<Household> findByHouseholdHeadNameContaining(@Param("name") String name);

    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM Household h WHERE h.parcel.id = :parcelId AND h.active = true")
    boolean existsActiveByParcelId(@Param("parcelId") Long parcelId);

    long countByActiveTrue();

    long countByParcelId(Long parcelId);

    @Query("""
           SELECT h FROM Household h
           LEFT JOIN FETCH h.parcel p
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority
           WHERE h.id = :id
           """)
    Optional<Household> findByIdWithRelations(@Param("id") Long id);

    @Query("""
           SELECT h FROM Household h
           LEFT JOIN FETCH h.parcel p
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority
           ORDER BY h.createdAt DESC
           """)
    List<Household> findAllOrderByCreatedAtDesc();
}