package za.co.taloms.pto.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.taloms.pto.domain.entity.PTOApprovalSignature;
import java.util.List;

public interface PTOApprovalSignatureJpaRepository extends JpaRepository<PTOApprovalSignature, Long> {

    List<PTOApprovalSignature> findByPtoIdOrderBySignedAtDesc(Long ptoId);

    List<PTOApprovalSignature> findBySignedByOrderBySignedAtDesc(String signedBy);
}
