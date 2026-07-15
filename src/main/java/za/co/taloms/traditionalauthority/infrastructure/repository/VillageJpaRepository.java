package za.co.taloms.traditionalauthority.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.taloms.traditionalauthority.domain.entity.Village;
import java.util.List;

public interface VillageJpaRepository
        extends JpaRepository<Village, Long> {

    List<Village> findByTraditionalAuthorityId(Long authorityId);

    boolean existsByVillageNameAndTraditionalAuthorityId(
            String villageName, Long authorityId);

    boolean existsByVillageNameAndTraditionalAuthorityIdAndIdNot(
            String villageName, Long authorityId, Long id);

    long countByTraditionalAuthorityId(Long authorityId);

    @Query("SELECT v FROM Village v WHERE v.active = true ORDER BY v.villageName")
    List<Village> findAllActive();

    @Query("SELECT v FROM Village v WHERE v.traditionalAuthority.id = :authorityId " +
            "ORDER BY v.villageName")
    List<Village> findByAuthorityIdOrdered(@Param("authorityId") Long authorityId);
}