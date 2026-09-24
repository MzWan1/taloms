package za.co.taloms.security.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.taloms.security.domain.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByIdNumber(String idNumber);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByIdNumber(String idNumber);

    /**
     * IDs of the authorities a user belongs to through the chief_authorities
     * join table. Kept as a projection query so authorization never triggers a
     * lazy collection (no N+1 on the user entity).
     */
    @Query("SELECT a.id FROM User u JOIN u.authorities a WHERE u.id = :userId")
    List<Long> findAuthorityIdsByUserId(@Param("userId") Long userId);

    /** Users (chiefs) linked to the given authority through the join table. */
    @Query("SELECT u FROM User u JOIN u.authorities a WHERE a.id = :authorityId ORDER BY u.fullName")
    List<User> findByAuthorityId(@Param("authorityId") Long authorityId);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    List<User> findByEnabledTrue();

    long countByEnabledTrue();

    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findAllOrderByCreatedAtDesc();

        @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "u.idNumber LIKE CONCAT('%', :query, '%') " +
           "ORDER BY u.fullName")
    List<User> searchByNameOrEmail(@Param("query") String query);

    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND EXISTS (SELECT 1 FROM User u2 JOIN u2.roles r " +
           "            WHERE u2 = u AND r.name = :roleName) " +
           "AND (:authorityId IS NULL OR u.traditionalAuthorityId IS NULL OR u.traditionalAuthorityId = :authorityId) " +
           "ORDER BY u.fullName")
    List<User> searchByNameOrEmailAndAuthorityScope(@Param("query") String query,
                                                     @Param("authorityId") Long authorityId,
                                                     @Param("roleName") String roleName);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND r.name = 'ROLE_COMPANY' " +
           "ORDER BY u.username")
    List<User> searchAvailableCompanyOwnersByNameOrEmail(@Param("query") String query);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE u.idNumber = :idNumber " +
           "AND r.name = 'ROLE_COMPANY'")
    List<User> searchAvailableCompanyOwnerByIdNumber(@Param("idNumber") String idNumber);
}

