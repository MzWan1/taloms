package za.co.taloms.company.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.company.application.dto.PorVerificationResponse;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.IdMasker;
import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;
import za.co.taloms.resident.domain.entity.Resident;
import za.co.taloms.resident.domain.repository.ResidentRepositoryPort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Verifies proof of residence from TALOMS data.
 *
 * In TALOMS a proof of residence is derived from an approved PTO record whose
 * holder's SA ID number matches the queried number (this mirrors the resident
 * portal's ownership model). A proof is valid when the PTO is ACTIVE, not
 * soft-deleted and not past its expiry date.
 *
 * Privacy: the response is existence-neutral — an unknown resident and a known
 * resident without a valid proof produce the identical result, so the API cannot
 * be used to test whether a person is known to TALOMS. Only the minimal fields
 * needed to confirm residence are returned.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProofOfResidenceServiceImpl implements ProofOfResidenceService {

    private static final Pattern SA_ID_PATTERN = Pattern.compile("\\d{13}");
    private static final String REASON_NOT_FOUND = "NOT_FOUND";
    private static final String MESSAGE_NOT_FOUND =
            "No valid proof of residence was found for the supplied ID number.";

    private final ResidentRepositoryPort residentRepository;
    private final PTORepositoryPort ptoRepository;

    @Override
    public PorVerificationResponse verify(String idNumber) {
        String normalized = idNumber == null ? "" : idNumber.trim();

        // Defence in depth: the DTO also validates, but the service must never
        // perform a lookup for a malformed ID. The message never echoes the value.
        if (!SA_ID_PATTERN.matcher(normalized).matches()) {
            throw new BusinessValidationException("ID number must contain exactly 13 digits.");
        }

        PTO proof = findValidProof(normalized);
        if (proof == null) {
            // Identical response for "unknown resident" and "no valid proof".
            return PorVerificationResponse.builder()
                    .verified(false)
                    .reason(REASON_NOT_FOUND)
                    .message(MESSAGE_NOT_FOUND)
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        Resident resident = residentRepository.findByIdNumber(normalized).orElse(null);
        String residentName = resident != null && resident.getFullName() != null
                ? resident.getFullName()
                : proof.getPtoHolderName();

        return PorVerificationResponse.builder()
                .verified(true)
                .residentName(residentName)
                .maskedIdNumber(IdMasker.maskIdNumber(normalized))
                .proofOfResidenceNumber(proof.getPtoNumber())
                .proofStatus(proof.getStatus() == null ? null : proof.getStatus().name())
                .issueDate(proof.getIssueDate())
                .expiryDate(proof.getExpiryDate())
                .villageName(proof.getVillage() == null ? null : proof.getVillage().getVillageName())
                .traditionalAuthorityName(proof.getTraditionalAuthority() == null
                        ? null
                        : proof.getTraditionalAuthority().getAuthorityName())
                .verifiedAt(LocalDateTime.now())
                .build();
    }

    private PTO findValidProof(String idNumber) {
        List<PTO> candidates = ptoRepository.findByIdNumberAndStatus(idNumber, PTOStatus.ACTIVE);
        LocalDate today = LocalDate.now();
        return candidates.stream()
                .filter(p -> !p.isDeleted())
                .filter(p -> p.getExpiryDate() == null || !p.getExpiryDate().isBefore(today))
                .findFirst()
                .orElse(null);
    }

}
