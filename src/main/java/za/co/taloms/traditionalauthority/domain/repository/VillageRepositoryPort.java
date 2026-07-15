package za.co.taloms.traditionalauthority.domain.repository;

import za.co.taloms.traditionalauthority.domain.entity.Village;
import java.util.List;
import java.util.Optional;

public interface VillageRepositoryPort {
    Village save(Village village);
    Optional<Village> findById(Long id);
    List<Village> findAll();
    List<Village> findByTraditionalAuthorityId(Long authorityId);
    List<Village> findAllActive();
    boolean existsByVillageNameAndTraditionalAuthorityId(
            String villageName, Long authorityId);
    boolean existsByVillageNameAndTraditionalAuthorityIdAndIdNot(
            String villageName, Long authorityId, Long excludeId);
    long countByTraditionalAuthorityId(Long authorityId);
}