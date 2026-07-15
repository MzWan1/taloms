package za.co.taloms.household.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.household.domain.entity.Household;
import za.co.taloms.household.domain.repository.HouseholdRepositoryPort;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HouseholdRepositoryAdapter implements HouseholdRepositoryPort {

    private final HouseholdJpaRepository jpaRepository;

    @Override
    public Household save(Household household) {
        return jpaRepository.save(household);
    }

    @Override
    public Optional<Household> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Household> findAll() {
        return jpaRepository.findAllOrderByCreatedAtDesc();
    }

    @Override
    public List<Household> findByParcelId(Long parcelId) {
        return jpaRepository.findByParcelId(parcelId);
    }

    @Override
    public List<Household> findByPtoId(Long ptoId) {
        return jpaRepository.findByPtoId(ptoId);
    }

    @Override
    public List<Household> findByActiveTrue() {
        return jpaRepository.findByActiveTrue();
    }

    @Override
    public Optional<Household> findActiveByParcelId(Long parcelId) {
        return jpaRepository.findActiveByParcelId(parcelId);
    }

    @Override
    public List<Household> findByHouseholdHeadNameContaining(String name) {
        return jpaRepository.findByHouseholdHeadNameContaining(name);
    }

    @Override
    public boolean existsActiveByParcelId(Long parcelId) {
        return jpaRepository.existsActiveByParcelId(parcelId);
    }

    @Override
    public long countByActiveTrue() {
        return jpaRepository.countByActiveTrue();
    }

    @Override
    public long countByParcelId(Long parcelId) {
        return jpaRepository.countByParcelId(parcelId);
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }
}