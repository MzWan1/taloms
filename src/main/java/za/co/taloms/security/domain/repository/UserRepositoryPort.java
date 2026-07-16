package za.co.taloms.security.domain.repository;

import za.co.taloms.security.domain.entity.Role;
import za.co.taloms.security.domain.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    List<User> findByRoleName(String roleName);
    List<User> findAllActive();
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    void delete(User user);
    long countAll();
    long countByActiveTrue();
    List<Role> findAllRoles();
}