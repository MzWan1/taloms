package za.co.taloms.security.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.taloms.security.domain.entity.User;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}