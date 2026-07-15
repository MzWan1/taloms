package za.co.taloms.pto.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.entity.PTOStatus;
import java.util.List;
import java.util.Optional;

public interface PTOJpaRepository extends JpaRepository<PTO, Long> {

    Optional<PTO> findByPtoNumber(String ptoNumber);

    boolean existsByPtoNumber(String ptoNumber);

    List<PTO> findByStatus(PTOStatus status);

    List<PTO> findByIdNumber(String idNumber);

    boolean existsByIdNumberAndStatus(
            String idNumber, PTOStatus status);

    long countByStatus(PTOStatus status);

    long countByTraditionalAuthorityId(Long authorityId);

    @Query("SELECT p FROM PTO p WHERE p.village.id = :villageId " +
            "ORDER BY p.createdAt DESC")
    List<PTO> findByVillageId(
            @Param("villageId") Long villageId);

    @Query("SELECT p FROM PTO p " +
            "WHERE p.traditionalAuthority.id = :authorityId " +
            "ORDER BY p.createdAt DESC")
    List<PTO> findByTraditionalAuthorityId(
            @Param("authorityId") Long authorityId);

    @Query("SELECT p FROM PTO p ORDER BY p.createdAt DESC")
    List<PTO> findAllOrderByCreatedAtDesc();
}