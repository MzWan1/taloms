package za.co.taloms.pto.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.pto.domain.entity.PTOApprovalSignature;
import za.co.taloms.pto.domain.repository.PTOApprovalSignatureRepositoryPort;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PTOApprovalSignatureRepositoryAdapter implements PTOApprovalSignatureRepositoryPort {

    private final PTOApprovalSignatureJpaRepository jpaRepository;

    @Override
    public PTOApprovalSignature save(PTOApprovalSignature signature) {
        return jpaRepository.save(signature);
    }

    @Override
    public List<PTOApprovalSignature> findByPtoId(Long ptoId) {
        return jpaRepository.findByPtoIdOrderBySignedAtDesc(ptoId);
    }

    @Override
    public List<PTOApprovalSignature> findBySignedBy(String signedBy) {
        return jpaRepository.findBySignedByOrderBySignedAtDesc(signedBy);
    }
}


