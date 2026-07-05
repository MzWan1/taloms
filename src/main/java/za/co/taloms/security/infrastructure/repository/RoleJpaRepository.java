package za.co.taloms.security.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.taloms.security.domain.entity.Role;
import java.util.Optional;

public interface RoleJpaRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}