package za.co.taloms.traditionalauthority.domain.repository;

import za.co.taloms.traditionalauthority.domain.entity.TraditionalAuthority;
import java.util.List;
import java.util.Optional;

public interface TraditionalAuthorityRepositoryPort {
    TraditionalAuthority save(TraditionalAuthority authority);
    Optional<TraditionalAuthority> findById(Long id);
    List<TraditionalAuthority> findAll();
    List<TraditionalAuthority> findAllActive();
    boolean existsByAuthorityName(String name);
    boolean existsByAuthorityNameAndIdNot(String name, Long id);
    boolean existsByHeadmanIdAndIdNot(Long headmanId, Long excludeId);

    boolean existsByHeadmanId(Long headmanId);

    /** IDs of authorities whose primary chief or headman is the given user. */
    List<Long> findIdsByChiefIdOrHeadmanId(Long userId);

    void delete(TraditionalAuthority authority);
}

