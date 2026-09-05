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

        /**
     * Search users by full name or email (case-insensitive, partial match).
     * Does not expose username.
     */
    List<User> searchByNameOrEmail(String query);

    /**
     * Search users by full name or email (case-insensitive, partial match),
     * additionally filtered to those whose traditionalAuthorityId matches
     * the given authority, or who are not linked to any authority.
     * This is used for the headsman/chief picker so that users already
     * serving under a different authority do not appear.
     */
    List<User> searchByNameOrEmailAndAuthorityScope(String query, Long authorityId);
}

