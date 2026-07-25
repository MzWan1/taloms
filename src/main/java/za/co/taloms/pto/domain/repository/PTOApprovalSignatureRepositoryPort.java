package za.co.taloms.pto.domain.repository;

import za.co.taloms.pto.domain.entity.PTOApprovalSignature;
import java.util.List;

public interface PTOApprovalSignatureRepositoryPort {
    PTOApprovalSignature save(PTOApprovalSignature signature);
    List<PTOApprovalSignature> findByPtoId(Long ptoId);
    List<PTOApprovalSignature> findBySignedBy(String signedBy);
}
