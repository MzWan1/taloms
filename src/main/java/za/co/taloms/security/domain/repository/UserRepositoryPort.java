package za.co.taloms.security.domain.repository;

import za.co.taloms.security.domain.entity.User;
import java.util.Optional;
import java.util.List;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findAll();
}