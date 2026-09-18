package za.co.taloms.useraccess.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.taloms.useraccess.domain.entity.UserPTOAccess;
import java.util.List;
import java.util.Optional;

public interface UserPTOAccessJpaRepository extends JpaRepository<UserPTOAccess, Long> {

    Optional<UserPTOAccess> findByUserIdAndPtoId(Long userId, Long ptoId);

    List<UserPTOAccess> findAllByPtoId(Long ptoId);

    List<UserPTOAccess> findAllByUserId(Long userId);

    boolean existsByUserIdAndPtoId(Long userId, Long ptoId);

    void deleteByUserIdAndPtoId(Long userId, Long ptoId);
}