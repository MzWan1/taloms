package za.co.taloms.household.domain.repository;

import za.co.taloms.household.domain.entity.Household;
import java.util.List;
import java.util.Optional;

public interface HouseholdRepositoryPort {
    Household save(Household household);
    Optional<Household> findById(Long id);
    List<Household> findAll();
    List<Household> findByParcelId(Long parcelId);
    List<Household> findByParcelIdIn(List<Long> parcelIds);
    List<Household> findByPtoId(Long ptoId);
    List<Household> findByActiveTrue();
    Optional<Household> findActiveByParcelId(Long parcelId);
    List<Household> findByHouseholdHeadNameContaining(String name);
    boolean existsActiveByParcelId(Long parcelId);
    long countByActiveTrue();
    long countByParcelId(Long parcelId);
    long countAll();
}