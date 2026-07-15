package za.co.taloms.traditionalauthority.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.traditionalauthority.domain.entity.Village;
import za.co.taloms.traditionalauthority.domain.repository.VillageRepositoryPort;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class VillageRepositoryAdapter
        implements VillageRepositoryPort {

    private final VillageJpaRepository jpaRepository;

    @Override
    public Village save(Village village) {
        return jpaRepository.save(village);
    }

    @Override
    public Optional<Village> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Village> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Village> findByTraditionalAuthorityId(Long authorityId) {
        return jpaRepository.findByAuthorityIdOrdered(authorityId);
    }

    @Override
    public List<Village> findAllActive() {
        return jpaRepository.findAllActive();
    }

    @Override
    public boolean existsByVillageNameAndTraditionalAuthorityId(
            String villageName, Long authorityId) {
        return jpaRepository
                .existsByVillageNameAndTraditionalAuthorityId(
                        villageName, authorityId);
    }

    @Override
    public boolean existsByVillageNameAndTraditionalAuthorityIdAndIdNot(
            String villageName, Long authorityId, Long excludeId) {
        return jpaRepository
                .existsByVillageNameAndTraditionalAuthorityIdAndIdNot(
                        villageName, authorityId, excludeId);
    }

    @Override
    public long countByTraditionalAuthorityId(Long authorityId) {
        return jpaRepository.countByTraditionalAuthorityId(authorityId);
    }
}