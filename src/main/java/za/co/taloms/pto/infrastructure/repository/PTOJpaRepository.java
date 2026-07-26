package za.co.taloms.pto.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    boolean existsByIdNumberAndStatus(String idNumber, PTOStatus status);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM PTO p WHERE p.idNumber = :idNumber " +
            "AND p.parcel.id = :parcelId " +
            "AND p.status = :status")
    boolean existsByIdNumberAndParcelIdAndStatus(
            @Param("idNumber") String idNumber,
            @Param("parcelId") Long parcelId,
            @Param("status") PTOStatus status);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM PTO p WHERE p.parcel.id = :parcelId " +
            "AND p.status = :status")
    boolean existsByParcelIdAndStatus(
            @Param("parcelId") Long parcelId,
            @Param("status") PTOStatus status);

    @Query("SELECT p FROM PTO p WHERE p.parcel.id = :parcelId ORDER BY p.createdAt DESC")
    List<PTO> findByParcelId(@Param("parcelId") Long parcelId);

    long countByStatus(PTOStatus status);

    long countByTraditionalAuthorityId(Long authorityId);

    long countByTraditionalAuthorityIdAndStatus(Long authorityId, PTOStatus status);

    long countByVillageIdAndStatus(Long villageId, PTOStatus status);

    long countByVillageId(Long villageId);

    @Query(value = "SELECT COUNT(*) FROM pto_records WHERE traditional_authority_id = :authorityId AND issue_date BETWEEN :dateFrom AND :dateTo", nativeQuery = true)
    long countByAuthorityIdAndIssueDateBetween(@Param("authorityId") Long authorityId, @Param("dateFrom") java.time.LocalDate dateFrom, @Param("dateTo") java.time.LocalDate dateTo);

    @Query(value = "SELECT COUNT(*) FROM pto_records WHERE village_id = :villageId AND issue_date BETWEEN :dateFrom AND :dateTo", nativeQuery = true)
    long countByVillageIdAndIssueDateBetween(@Param("villageId") Long villageId, @Param("dateFrom") java.time.LocalDate dateFrom, @Param("dateTo") java.time.LocalDate dateTo);

    @Query(value = "SELECT COUNT(*) FROM pto_records WHERE issue_date BETWEEN :dateFrom AND :dateTo", nativeQuery = true)
    long countByIssueDateBetween(@Param("dateFrom") java.time.LocalDate dateFrom, @Param("dateTo") java.time.LocalDate dateTo);

    @Query("SELECT p FROM PTO p WHERE p.village.id = :villageId ORDER BY p.createdAt DESC")
    List<PTO> findByVillageId(@Param("villageId") Long villageId);

    @Query("SELECT p FROM PTO p WHERE p.traditionalAuthority.id = :authorityId ORDER BY p.createdAt DESC")
    List<PTO> findByTraditionalAuthorityId(@Param("authorityId") Long authorityId);

    @Query("SELECT p FROM PTO p WHERE p.idNumber = :idNumber AND p.status = :status ORDER BY p.createdAt DESC")
    List<PTO> findByIdNumberAndStatus(@Param("idNumber") String idNumber, @Param("status") PTOStatus status);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PTO p WHERE p.idNumber = :idNumber AND p.village.id = :villageId AND p.status = :status")
    boolean existsByIdNumberAndVillageIdAndStatus(@Param("idNumber") String idNumber, @Param("villageId") Long villageId, @Param("status") PTOStatus status);

    @Query("SELECT p FROM PTO p ORDER BY p.createdAt DESC")
    List<PTO> findAllOrderByCreatedAtDesc();

    @Query("""
            SELECT p FROM PTO p WHERE
            (:holderName IS NULL OR LOWER(p.ptoHolderName) LIKE LOWER(CONCAT('%', :holderName, '%')))
            AND (:idNumber IS NULL OR p.idNumber LIKE CONCAT('%', :idNumber, '%'))
            AND (:ptoNumber IS NULL OR LOWER(p.ptoNumber) LIKE LOWER(CONCAT('%', :ptoNumber, '%')))
            AND (:status IS NULL OR p.status = :status)
            AND (:purpose IS NULL OR p.purpose = :purpose)
            AND (:villageId IS NULL OR p.village.id = :villageId)
            AND (:authorityId IS NULL OR p.traditionalAuthority.id = :authorityId)
            AND p.deletedAt IS NULL
            ORDER BY p.createdAt DESC
            """)
    List<PTO> search(@Param("holderName") String holderName,
                     @Param("idNumber") String idNumber,
                     @Param("ptoNumber") String ptoNumber,
                     @Param("status") PTOStatus status,
                     @Param("purpose") za.co.taloms.pto.domain.entity.PTOPurpose purpose,
                     @Param("villageId") Long villageId,
                     @Param("authorityId") Long authorityId);

    @Modifying
    @Query(value = "UPDATE pto_records SET deleted_at = CURRENT_TIMESTAMP, deleted_by = :deletedBy, status = 'REVOKED', revoked_by = :deletedBy, revoked_at = CURRENT_TIMESTAMP, revoke_reason = 'PTO record deleted by ' || :deletedBy WHERE id = :id", nativeQuery = true)
    void softDeleteById(@Param("id") Long id, @Param("deletedBy") String deletedBy);

    @Query("SELECT p FROM PTO p WHERE p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<PTO> findAllIncludingDeleted();

    @Query("SELECT p FROM PTO p WHERE p.deletedAt IS NOT NULL ORDER BY p.deletedAt DESC")
    List<PTO> findDeleted();
}

