package za.co.taloms.resident.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.taloms.resident.domain.entity.Resident;
import za.co.taloms.resident.domain.entity.RelationshipType;
import java.util.List;
import java.util.Optional;

public interface ResidentJpaRepository extends JpaRepository<Resident, Long> {

    Optional<Resident> findByIdNumber(String idNumber);

    boolean existsByIdNumber(String idNumber);

    List<Resident> findByHouseholdId(Long householdId);

    List<Resident> findByActiveTrue();

    List<Resident> findByRelationshipType(RelationshipType relationshipType);

    @Query("SELECT r FROM Resident r WHERE LOWER(r.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Resident> findByFullNameContaining(@Param("name") String name);

    long countByHouseholdId(Long householdId);

    long countByActiveTrue();

    @Query("SELECT r FROM Resident r ORDER BY r.createdAt DESC")
    List<Resident> findAllOrderByCreatedAtDesc();
}