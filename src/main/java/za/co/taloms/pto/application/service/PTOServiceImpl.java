package za.co.taloms.pto.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.DuplicateRecordException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.pto.application.dto.*;
import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.entity.PTOPurpose;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;
import za.co.taloms.traditionalauthority.domain.repository.TraditionalAuthorityRepositoryPort;
import za.co.taloms.traditionalauthority.domain.repository.VillageRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import za.co.taloms.pto.domain.event.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PTOServiceImpl implements PTOService {

    private final PTORepositoryPort                  ptoRepository;
    private final PTONumberGenerator                 numberGenerator;
    private final TraditionalAuthorityRepositoryPort authorityRepository;
    private final VillageRepositoryPort             villageRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PTOResponse createPTO(PTORequest request,
                                 String createdBy) {
        // Validate no active PTO for same ID number
        if (ptoRepository.existsByIdNumberAndStatus(
                request.getIdNumber(), PTOStatus.ACTIVE)) {
            throw new BusinessValidationException(
                    "An active PTO already exists for ID number: "
                            + request.getIdNumber());
        }

        var authority = authorityRepository
                .findById(request.getTraditionalAuthorityId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Traditional Authority",
                        request.getTraditionalAuthorityId()));

        var village = villageRepository
                .findById(request.getVillageId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Village", request.getVillageId()));

        String ptoNumber = numberGenerator.generate();
        while (ptoRepository.existsByPtoNumber(ptoNumber)) {
            ptoNumber = numberGenerator.generate();
        }

        var pto = PTO.builder()
                .ptoNumber(ptoNumber)
                .ptoHolderName(request.getPtoHolderName())
                .idNumber(request.getIdNumber())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .purpose(PTOPurpose.valueOf(request.getPurpose()))
                .status(PTOStatus.PENDING)
                .issueDate(request.getIssueDate())
                .expiryDate(request.getExpiryDate())
                .notes(request.getNotes())
                .village(village)
                .traditionalAuthority(authority)
                .createdBy(createdBy)
                .build();

        var saved = ptoRepository.save(pto);
        // Publish event
        eventPublisher.publishEvent(new PTOCreatedEvent(
                this,
                saved.getId(),
                saved.getPtoNumber(),
                saved.getPtoHolderName(),
                saved.getVillage().getId(),
                saved.getTraditionalAuthority().getId(),
                createdBy));

        log.info("Created PTO: {} for holder: {} by {}",
                saved.getPtoNumber(),
                saved.getPtoHolderName(),
                createdBy);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PTOResponse findById(Long id) {
        return ptoRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PTO", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PTOResponse findByPtoNumber(String ptoNumber) {
        return ptoRepository.findByPtoNumber(ptoNumber)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PTO not found: " + ptoNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PTOResponse> findAll() {
        return ptoRepository.findAll()
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PTOResponse> findByStatus(PTOStatus status) {
        return ptoRepository.findByStatus(status)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PTOResponse> findByAuthority(Long authorityId) {
        return ptoRepository.findByTraditionalAuthorityId(authorityId)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PTOResponse> findByVillage(Long villageId) {
        return ptoRepository.findByVillageId(villageId)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PTOResponse> search(PTOSearchCriteria criteria) {
        return ptoRepository.findAll().stream()
                .filter(p -> criteria.getHolderName() == null
                        || p.getPtoHolderName().toLowerCase()
                        .contains(criteria.getHolderName().toLowerCase()))
                .filter(p -> criteria.getIdNumber() == null
                        || p.getIdNumber()
                        .contains(criteria.getIdNumber()))
                .filter(p -> criteria.getPtoNumber() == null
                        || p.getPtoNumber().toLowerCase()
                        .contains(criteria.getPtoNumber().toLowerCase()))
                .filter(p -> criteria.getStatus() == null
                        || p.getStatus() == criteria.getStatus())
                .filter(p -> criteria.getPurpose() == null
                        || p.getPurpose() == criteria.getPurpose())
                .filter(p -> criteria.getVillageId() == null
                        || (p.getVillage() != null
                        && p.getVillage().getId()
                        .equals(criteria.getVillageId())))
                .filter(p -> criteria.getAuthorityId() == null
                        || (p.getTraditionalAuthority() != null
                        && p.getTraditionalAuthority().getId()
                        .equals(criteria.getAuthorityId())))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PTOResponse approvePTO(Long id,
                                  PTOApprovalRequest request,
                                  String approvedBy) {
        var pto = ptoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PTO", id));

        if (!pto.canBeApproved()) {
            throw new BusinessValidationException(
                    "PTO cannot be approved in status: "
                            + pto.getStatus().getDisplayName());
        }

        pto.setStatus(PTOStatus.ACTIVE);
        pto.setApprovedBy(approvedBy);
        pto.setApprovedAt(LocalDateTime.now());
        if (request.getNotes() != null) {
            pto.setNotes(request.getNotes());
        }

        var saved = ptoRepository.save(pto);

        eventPublisher.publishEvent(new PTOApprovedEvent(
                this,
                saved.getId(),
                saved.getPtoNumber(),
                saved.getPtoHolderName(),
                approvedBy,
                saved.getApprovedAt()));

        log.info("PTO {} approved by {}",
                saved.getPtoNumber(), approvedBy);
        return toResponse(saved);
    }

    @Override
    public PTOResponse revokePTO(Long id,
                                 PTORevokeRequest request,
                                 String revokedBy) {
        var pto = ptoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PTO", id));

        if (!pto.canBeRevoked()) {
            throw new BusinessValidationException(
                    "PTO cannot be revoked in status: "
                            + pto.getStatus().getDisplayName());
        }

        pto.setStatus(PTOStatus.REVOKED);
        pto.setRevokedBy(revokedBy);
        pto.setRevokedAt(LocalDateTime.now());
        pto.setRevokeReason(request.getReason());

        var saved = ptoRepository.save(pto);

        eventPublisher.publishEvent(new PTORevokedEvent(
                this,
                saved.getId(),
                saved.getPtoNumber(),
                saved.getPtoHolderName(),
                revokedBy,
                request.getReason(),
                saved.getRevokedAt()));

        log.info("PTO {} revoked by {} — reason: {}",
                saved.getPtoNumber(),
                revokedBy,
                request.getReason());
        return toResponse(saved);
    }

    @Override
    public PTOResponse suspendPTO(Long id, String suspendedBy) {
        var pto = ptoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PTO", id));

        if (!pto.canBeSuspended()) {
            throw new BusinessValidationException(
                    "PTO cannot be suspended in status: "
                            + pto.getStatus().getDisplayName());
        }

        pto.setStatus(PTOStatus.SUSPENDED);
        var saved = ptoRepository.save(pto);
        log.info("PTO {} suspended by {}",
                saved.getPtoNumber(), suspendedBy);
        return toResponse(saved);
    }

    @Override
    public PTOResponse reactivatePTO(Long id, String reactivatedBy) {
        var pto = ptoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PTO", id));

        if (pto.getStatus() != PTOStatus.SUSPENDED) {
            throw new BusinessValidationException(
                    "Only SUSPENDED PTOs can be reactivated");
        }

        pto.setStatus(PTOStatus.ACTIVE);
        var saved = ptoRepository.save(pto);
        log.info("PTO {} reactivated by {}",
                saved.getPtoNumber(), reactivatedBy);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(PTOStatus status) {
        return ptoRepository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return ptoRepository.countAll();
    }

    private PTOResponse toResponse(PTO p) {
        return PTOResponse.builder()
                .id(p.getId())
                .ptoNumber(p.getPtoNumber())
                .ptoHolderName(p.getPtoHolderName())
                .idNumber(p.getIdNumber())
                .contactPhone(p.getContactPhone())
                .contactEmail(p.getContactEmail())
                .purpose(p.getPurpose())
                .purposeDisplay(p.getPurpose().getDisplayName())
                .status(p.getStatus())
                .statusDisplay(p.getStatus().getDisplayName())
                .statusBadgeClass(p.getStatus().getBadgeClass())
                .issueDate(p.getIssueDate())
                .expiryDate(p.getExpiryDate())
                .notes(p.getNotes())
                .villageId(p.getVillage() != null
                        ? p.getVillage().getId() : null)
                .villageName(p.getVillage() != null
                        ? p.getVillage().getVillageName() : null)
                .traditionalAuthorityId(p.getTraditionalAuthority() != null
                        ? p.getTraditionalAuthority().getId() : null)
                .authorityName(p.getTraditionalAuthority() != null
                        ? p.getTraditionalAuthority().getAuthorityName() : null)
                .approvedBy(p.getApprovedBy())
                .approvedAt(p.getApprovedAt())
                .revokedBy(p.getRevokedBy())
                .revokedAt(p.getRevokedAt())
                .revokeReason(p.getRevokeReason())
                .createdBy(p.getCreatedBy())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}