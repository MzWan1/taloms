package za.co.taloms.pto.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PTORepositoryAdapter implements PTORepositoryPort {

    private final PTOJpaRepository jpaRepository;

    @Override
    public PTO save(PTO pto) {
        return jpaRepository.save(pto);
    }

    @Override
    public Optional<PTO> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<PTO> findByPtoNumber(String ptoNumber) {
        return jpaRepository.findByPtoNumber(ptoNumber);
    }

    @Override
    public List<PTO> findAll() {
        return jpaRepository.findAllOrderByCreatedAtDesc();
    }

    @Override
    public List<PTO> findByStatus(PTOStatus status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public List<PTO> findByVillageId(Long villageId) {
        return jpaRepository.findByVillageId(villageId);
    }

    @Override
    public List<PTO> findByTraditionalAuthorityId(Long authorityId) {
        return jpaRepository.findByTraditionalAuthorityId(authorityId);
    }

    @Override
    public List<PTO> findByIdNumber(String idNumber) {
        return jpaRepository.findByIdNumber(idNumber);
    }

    @Override
    public boolean existsByPtoNumber(String ptoNumber) {
        return jpaRepository.existsByPtoNumber(ptoNumber);
    }

    @Override
    public boolean existsByIdNumberAndStatus(
            String idNumber, PTOStatus status) {
        return jpaRepository.existsByIdNumberAndStatus(
                idNumber, status);
    }

    @Override
    public long countByStatus(PTOStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public long countByTraditionalAuthorityId(Long authorityId) {
        return jpaRepository.countByTraditionalAuthorityId(authorityId);
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }
}