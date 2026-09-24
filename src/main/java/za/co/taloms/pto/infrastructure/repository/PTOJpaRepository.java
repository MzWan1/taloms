package za.co.taloms.pto.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.entity.PTOStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    Page<PTO> findByTraditionalAuthorityId(@Param("authorityId") Long authorityId, Pageable pageable);

    @Query("SELECT p FROM PTO p WHERE p.idNumber = :idNumber AND p.status = :status ORDER BY p.createdAt DESC")
    List<PTO> findByIdNumberAndStatus(@Param("idNumber") String idNumber, @Param("status") PTOStatus status);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PTO p WHERE p.idNumber = :idNumber AND p.village.id = :villageId AND p.status = :status")
    boolean existsByIdNumberAndVillageIdAndStatus(@Param("idNumber") String idNumber, @Param("villageId") Long villageId, @Param("status") PTOStatus status);

    @Query("SELECT p FROM PTO p ORDER BY p.createdAt DESC")
    Page<PTO> findAllOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT p FROM PTO p WHERE
            (:filterByHolder = false OR LOWER(p.ptoHolderName) LIKE LOWER(CONCAT('%', :holderName, '%')))
            AND (:filterById = false OR p.idNumber LIKE CONCAT('%', :idNumber, '%'))
            AND (:filterByPto = false OR LOWER(p.ptoNumber) LIKE LOWER(CONCAT('%', :ptoNumber, '%')))
            AND (:filterByStatus = false OR p.status = :status)
            AND (:filterByPurpose = false OR p.purpose = :purpose)
            AND (:filterByVillages = false OR p.village.id IN :villageIds)
            AND (:filterByAuthority = false OR p.traditionalAuthority.id = :authorityId)
            AND p.deletedAt IS NULL
            ORDER BY p.createdAt DESC
            """)
    Page<PTO> search(@Param("holderName") String holderName, @Param("filterByHolder") boolean filterByHolder,
                     @Param("idNumber") String idNumber, @Param("filterById") boolean filterById,
                     @Param("ptoNumber") String ptoNumber, @Param("filterByPto") boolean filterByPto,
                     @Param("status") PTOStatus status, @Param("filterByStatus") boolean filterByStatus,
                     @Param("purpose") za.co.taloms.pto.domain.entity.PTOPurpose purpose, @Param("filterByPurpose") boolean filterByPurpose,
                     @Param("villageIds") Set<Long> villageIds, @Param("filterByVillages") boolean filterByVillages,
                     @Param("authorityId") Long authorityId, @Param("filterByAuthority") boolean filterByAuthority,
                     Pageable pageable);

    @Modifying
    @Query(value = "UPDATE pto_records SET deleted_at = CURRENT_TIMESTAMP, deleted_by = :deletedBy, status = 'REVOKED', revoked_by = :deletedBy, revoked_at = CURRENT_TIMESTAMP, revoke_reason = 'PTO record deleted by ' || :deletedBy WHERE id = :id", nativeQuery = true)
    void softDeleteById(@Param("id") Long id, @Param("deletedBy") String deletedBy);

    @Query("SELECT p FROM PTO p WHERE p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<PTO> findAllIncludingDeleted();

    @Query("SELECT p FROM PTO p WHERE p.deletedAt IS NOT NULL ORDER BY p.deletedAt DESC")
    Page<PTO> findDeleted(Pageable pageable);

    @Query("SELECT p FROM PTO p WHERE p.deletedAt IS NOT NULL AND (COALESCE(:villageIds, NULL) IS NULL OR p.village.id IN :villageIds) ORDER BY p.deletedAt DESC")
    Page<PTO> findDeletedScoped(@Param("villageIds") Set<Long> villageIds, Pageable pageable);

    // Sync & Batch operations
    @Query("""
            SELECT p FROM PTO p
            LEFT JOIN FETCH p.village v
            LEFT JOIN FETCH v.traditionalAuthority ta
            WHERE (:since IS NULL OR p.updatedAt >= :since)
            ORDER BY p.updatedAt ASC
            LIMIT :limit
            """)
    List<PTO> findChangedSince(@Param("since") LocalDateTime since, @Param("limit") int limit);

    @Query("""
            SELECT p FROM PTO p
            LEFT JOIN FETCH p.village v
            LEFT JOIN FETCH v.traditionalAuthority ta
            WHERE p.id IN :ids
            """)
    List<PTO> findByIds(@Param("ids") Set<Long> ids);
}


