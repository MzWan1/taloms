package za.co.taloms.resident.domain.repository;

import za.co.taloms.resident.domain.entity.Resident;
import za.co.taloms.resident.domain.entity.RelationshipType;
import java.util.List;
import java.util.Optional;

public interface ResidentRepositoryPort {
    Resident save(Resident resident);
    Optional<Resident> findById(Long id);
    Optional<Resident> findByIdNumber(String idNumber);
    List<Resident> findAll();
    List<Resident> findByHouseholdId(Long householdId);
    List<Resident> findByActiveTrue();
    List<Resident> findByFullNameContaining(String name);
    List<Resident> findByRelationshipType(RelationshipType relationshipType);
    boolean existsByIdNumber(String idNumber);
    long countByHouseholdId(Long householdId);
    long countByActiveTrue();
    long countAll();
}