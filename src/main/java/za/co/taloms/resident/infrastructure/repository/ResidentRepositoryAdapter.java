package za.co.taloms.resident.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.resident.domain.entity.Resident;
import za.co.taloms.resident.domain.entity.RelationshipType;
import za.co.taloms.resident.domain.repository.ResidentRepositoryPort;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ResidentRepositoryAdapter implements ResidentRepositoryPort {

    private final ResidentJpaRepository jpaRepository;

    @Override
    public Resident save(Resident resident) {
        return jpaRepository.save(resident);
    }

    @Override
    public Optional<Resident> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Resident> findByIdNumber(String idNumber) {
        return jpaRepository.findByIdNumber(idNumber);
    }

    @Override
    public List<Resident> findAll() {
        return jpaRepository.findAllOrderByCreatedAtDesc();
    }

    @Override
    public List<Resident> findByHouseholdId(Long householdId) {
        return jpaRepository.findByHouseholdId(householdId);
    }

    @Override
    public List<Resident> findByActiveTrue() {
        return jpaRepository.findByActiveTrue();
    }

    @Override
    public List<Resident> findByFullNameContaining(String name) {
        return jpaRepository.findByFullNameContaining(name);
    }

    @Override
    public List<Resident> findByRelationshipType(RelationshipType relationshipType) {
        return jpaRepository.findByRelationshipType(relationshipType);
    }

    @Override
    public boolean existsByIdNumber(String idNumber) {
        return jpaRepository.existsByIdNumber(idNumber);
    }

    @Override
    public long countByHouseholdId(Long householdId) {
        return jpaRepository.countByHouseholdId(householdId);
    }

    @Override
    public long countByActiveTrue() {
        return jpaRepository.countByActiveTrue();
    }

    @Override
    public long countByGender(za.co.taloms.resident.domain.entity.Gender gender) {
        return jpaRepository.countByGender(gender);
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }
}