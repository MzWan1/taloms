package za.co.taloms.pto.domain.repository;

import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.entity.PTOStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PTORepositoryPort {
    PTO save(PTO pto);
    Optional<PTO> findById(Long id);
    Optional<PTO> findByPtoNumber(String ptoNumber);
    List<PTO> findAll();
    Page<PTO> findAll(Pageable pageable);
    List<PTO> findByStatus(PTOStatus status);
    List<PTO> findByVillageId(Long villageId);
    Page<PTO> findByTraditionalAuthorityId(Long authorityId, Pageable pageable);
    List<PTO> findByIdNumber(String idNumber);
    List<PTO> findByParcelId(Long parcelId);
    boolean existsByPtoNumber(String ptoNumber);
    boolean existsByIdNumberAndStatus(String idNumber, PTOStatus status);
    boolean existsByIdNumberAndParcelIdAndStatus(String idNumber, Long parcelId, PTOStatus status);
    boolean existsByParcelIdAndStatus(Long parcelId, PTOStatus status);  // Check if parcel has PTO with specific status
    long countByStatus(PTOStatus status);
    long countByTraditionalAuthorityId(Long authorityId);
    long countByTraditionalAuthorityIdAndStatus(Long authorityId, PTOStatus status);
    long countByVillageIdAndStatus(Long villageId, PTOStatus status);
    long countByVillageId(Long villageId);
    long countByAuthorityIdAndIssueDateBetween(Long authorityId, java.time.LocalDate dateFrom, java.time.LocalDate dateTo);
    long countByVillageIdAndIssueDateBetween(Long villageId, java.time.LocalDate dateFrom, java.time.LocalDate dateTo);
    long countByIssueDateBetween(java.time.LocalDate dateFrom, java.time.LocalDate dateTo);
    long countAll();
    List<PTO> findByIdNumberAndStatus(String idNumber, PTOStatus status);
    boolean existsByIdNumberAndVillageIdAndStatus(String idNumber, Long villageId, PTOStatus status);
    Page<PTO> search(String holderName, String idNumber, String ptoNumber, PTOStatus status,
                     za.co.taloms.pto.domain.entity.PTOPurpose purpose, Set<Long> villageIds, Long authorityId, Pageable pageable);
    void softDeleteById(Long id, String deletedBy);
    List<PTO> findAllIncludingDeleted();
    Page<PTO> findDeleted(Pageable pageable);
    Page<PTO> findDeletedScoped(Set<Long> villageIds, Pageable pageable);

    // Sync operations
    List<PTO> findChangedSince(java.time.LocalDateTime since, int limit);
    List<PTO> findByIds(Set<Long> ids);
}

