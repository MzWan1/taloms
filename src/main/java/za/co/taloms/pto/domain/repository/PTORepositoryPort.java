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
    boolean existsByParcelIdAndStatus(Long parcelId, PTOStatus status);
    long countByStatus(PTOStatus status);
    long countByTraditionalAuthorityId(Long authorityId);
    long countAll();
}