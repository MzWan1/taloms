package za.co.taloms.household.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.taloms.household.domain.entity.Household;
import java.util.List;
import java.util.Optional;

public interface HouseholdJpaRepository extends JpaRepository<Household, Long> {

    List<Household> findByParcelId(Long parcelId);

    List<Household> findByPtoId(Long ptoId);

    List<Household> findByActiveTrue();

    @Query("SELECT h FROM Household h WHERE h.parcel.id = :parcelId AND h.active = true")
    Optional<Household> findActiveByParcelId(@Param("parcelId") Long parcelId);

    @Query("SELECT h FROM Household h WHERE LOWER(h.householdHeadName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Household> findByHouseholdHeadNameContaining(@Param("name") String name);

    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM Household h WHERE h.parcel.id = :parcelId AND h.active = true")
    boolean existsActiveByParcelId(@Param("parcelId") Long parcelId);

    long countByActiveTrue();

    long countByParcelId(Long parcelId);

    @Query("SELECT h FROM Household h ORDER BY h.createdAt DESC")
    List<Household> findAllOrderByCreatedAtDesc();
}