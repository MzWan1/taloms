package za.co.taloms.traditionalauthority.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import za.co.taloms.traditionalauthority.domain.entity.TraditionalAuthority;
import java.util.List;

public interface TraditionalAuthorityJpaRepository
        extends JpaRepository<TraditionalAuthority, Long> {

    boolean existsByAuthorityName(String authorityName);

    boolean existsByAuthorityNameAndIdNot(String authorityName, Long id);

    @Query("SELECT t FROM TraditionalAuthority t WHERE t.active = true ORDER BY t.authorityName")
    List<TraditionalAuthority> findAllActive();

    @Query("SELECT t FROM TraditionalAuthority t ORDER BY t.authorityName")
    List<TraditionalAuthority> findAllOrderByName();
}