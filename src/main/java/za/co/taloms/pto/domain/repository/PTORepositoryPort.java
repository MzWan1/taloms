package za.co.taloms.pto.domain.repository;

import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.entity.PTOStatus;
import java.util.List;
import java.util.Optional;

public interface PTORepositoryPort {
    PTO save(PTO pto);
    Optional<PTO> findById(Long id);
    Optional<PTO> findByPtoNumber(String ptoNumber);
    List<PTO> findAll();
    List<PTO> findByStatus(PTOStatus status);
    List<PTO> findByVillageId(Long villageId);
    List<PTO> findByTraditionalAuthorityId(Long authorityId);
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
    List<PTO> search(String holderName, String idNumber, String ptoNumber, PTOStatus status,
                     za.co.taloms.pto.domain.entity.PTOPurpose purpose, Long villageId, Long authorityId);
}