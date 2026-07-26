package za.co.taloms.pto.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.document.application.service.DocumentService;
import za.co.taloms.document.domain.entity.DocumentType;
import za.co.taloms.document.domain.entity.EntityType;
import za.co.taloms.parcel.domain.repository.ParcelRepositoryPort;
import za.co.taloms.pto.domain.repository.PTOApprovalSignatureRepositoryPort;
import za.co.taloms.pto.application.dto.*;
import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.entity.PTOPurpose;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.pto.domain.event.*;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;
import za.co.taloms.traditionalauthority.domain.repository.TraditionalAuthorityRepositoryPort;
import za.co.taloms.traditionalauthority.domain.repository.VillageRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PTOServiceImpl implements PTOService {

    private final PTORepositoryPort ptoRepository;
    private final ParcelRepositoryPort parcelRepository;
    private final PTONumberGenerator numberGenerator;
    private final TraditionalAuthorityRepositoryPort authorityRepository;
    private final VillageRepositoryPort villageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final DocumentService documentService;
    private final PTOApprovalSignatureRepositoryPort signatureRepository;

    @Override
    public PTOResponse createPTO(PTORequest request, String createdBy) {
        // ===== VALIDATION 1: Traditional Authority must exist =====
        var authority = authorityRepository.findById(request.getTraditionalAuthorityId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Traditional Authority", request.getTraditionalAuthorityId()));

        // ===== VALIDATION 2: Village must exist AND belong to the Authority =====
        var village = villageRepository.findById(request.getVillageId())
                .orElseThrow(() -> new ResourceNotFoundException("Village", request.getVillageId()));

        if (!village.getTraditionalAuthority().getId().equals(authority.getId())) {
            throw new BusinessValidationException(
                    "Village '" + village.getVillageName() +
                            "' does not belong to Traditional Authority '" + authority.getAuthorityName() + "'");
        }

        // ===== VALIDATION 3: Parcel must exist in this village =====
        var parcel = parcelRepository.findById(request.getParcelId())
                .orElseThrow(() -> new ResourceNotFoundException("Parcel", request.getParcelId()));

        if (parcel.getVillage() == null || !parcel.getVillage().getId().equals(village.getId())) {
            throw new BusinessValidationException(
                    "Parcel " + parcel.getParcelNumber() +
                            " does not belong to village '" + village.getVillageName() + "'");
        }

        // ===== VALIDATION 4: Stand number must match =====
        if (!parcel.getStandNumber().equals(request.getStandNumber())) {
            throw new BusinessValidationException(
                    "Stand number mismatch. Parcel " + parcel.getParcelNumber() +
                            " has stand number '" + parcel.getStandNumber() +
                            "' but request specifies '" + request.getStandNumber() + "'");
        }

        // ===== VALIDATION 5: Check if the parcel already has an ACTIVE PTO =====
        if (ptoRepository.existsByParcelIdAndStatus(request.getParcelId(), PTOStatus.ACTIVE)) {
            throw new BusinessValidationException(
                    "This parcel already has an ACTIVE PTO. Parcel: " + parcel.getParcelNumber() +
                            " - Stand: " + parcel.getStandNumber());
        }

        // ===== VALIDATION 6: Check if the parcel already has a SUSPENDED PTO =====
        if (ptoRepository.existsByParcelIdAndStatus(request.getParcelId(), PTOStatus.SUSPENDED)) {
            throw new BusinessValidationException(
                    "This parcel has a SUSPENDED PTO. Please reactivate or revoke the existing PTO first. " +
                            "Parcel: " + parcel.getParcelNumber() + " - Stand: " + parcel.getStandNumber());
        }

        // ===== VALIDATION 7: Check if this person already has an ACTIVE PTO on THIS parcel =====
        if (ptoRepository.existsByIdNumberAndParcelIdAndStatus(
                request.getIdNumber(),
                request.getParcelId(),
                PTOStatus.ACTIVE)) {
            throw new BusinessValidationException(
                    "This person already has an ACTIVE PTO on this parcel. " +
                            "Parcel: " + parcel.getParcelNumber() + " - Stand: " + parcel.getStandNumber());
        }

        // ===== VALIDATION 8: Check if this person already has a SUSPENDED PTO on THIS parcel =====
        if (ptoRepository.existsByIdNumberAndParcelIdAndStatus(
                request.getIdNumber(),
                request.getParcelId(),
                PTOStatus.SUSPENDED)) {
            throw new BusinessValidationException(
                    "This person has a SUSPENDED PTO on this parcel. Please reactivate or revoke it first.");
        }

        // ===== VALIDATION 9: Check if the parcel status is AVAILABLE =====
        if (!parcel.isAvailable()) {
            throw new BusinessValidationException(
                    "Parcel is not available for PTO allocation. Current status: " +
                            parcel.getStatus().getDisplayName());
        }

        // ===== VALIDATION 10: Community resolution required for AGRICULTURAL / BUSINESS =====
        boolean resolutionRequired = request.getCommunityResolutionRequired() != null
                && request.getCommunityResolutionRequired()
                && (request.getPurpose() != null
                && (request.getPurpose().equalsIgnoreCase("AGRICULTURAL")
                || request.getPurpose().equalsIgnoreCase("BUSINESS")));

        // Generate unique PTO number
        String ptoNumber = numberGenerator.generate();
        while (ptoRepository.existsByPtoNumber(ptoNumber)) {
            ptoNumber = numberGenerator.generate();
        }

        // Create PTO
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
                .parcel(parcel)
                .allocatedBy(request.getAllocatedBy())
                .allocationDate(request.getAllocationDate())
                .standArea(request.getStandArea())
                .surveyReference(request.getSurveyReference())
                .boundaryDescription(request.getBoundaryDescription())
                .allocationFeeReceipt(request.getAllocationFeeReceipt())
                .taRecommendationRef(request.getTaRecommendationRef())
                .communityResolutionRequired(resolutionRequired)
                .createdBy(createdBy)
                .build();

        var saved = ptoRepository.save(pto);

        // Publish event
        eventPublisher.publishEvent(new PTOCreatedEvent(
                this, saved.getId(), saved.getPtoNumber(),
                saved.getPtoHolderName(), saved.getVillage().getId(),
                saved.getTraditionalAuthority().getId(), createdBy));

        log.info("Created PTO: {} for holder: {} on parcel: {} ({}) by {}",
                saved.getPtoNumber(), saved.getPtoHolderName(),
                parcel.getParcelNumber(), parcel.getStandNumber(), createdBy);

        return toResponse(saved);
    }

    @Override
    public PTOResponse approvePTO(Long id, PTOApprovalRequest request, String approvedBy) {
        var pto = ptoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PTO", id));

        if (!pto.canBeApproved()) {
            throw new BusinessValidationException("PTO cannot be approved in status: " + pto.getStatus().getDisplayName());
        }

        var missingDocs = documentService.getMissingRequiredDocumentTypes(EntityType.PTO, id);
        if (!missingDocs.isEmpty()) {
            throw new BusinessValidationException(
                    "Cannot approve PTO: required documents missing. Missing: " +
                            missingDocs.stream().map(DocumentType::getDisplayName).collect(Collectors.joining(", ")));
        }

        // Check if the parcel is still available (no other active PTOs)
        if (pto.getParcel() != null && ptoRepository.existsByParcelIdAndStatus(pto.getParcel().getId(), PTOStatus.ACTIVE)) {
            throw new BusinessValidationException(
                    "Cannot approve: This parcel already has an ACTIVE PTO.");
        }

        pto.setStatus(PTOStatus.ACTIVE);
        pto.setApprovedBy(approvedBy);
        pto.setApprovedAt(LocalDateTime.now());
        if (request.getNotes() != null) {
            pto.setApprovalNotes(request.getNotes());
        }

        var saved = ptoRepository.save(pto);

        // Save e-signature if provided
        if (request.getSignatureData() != null || request.getSignatureImagePath() != null) {
            var signature = za.co.taloms.pto.domain.entity.PTOApprovalSignature.builder()
                    .ptoId(saved.getId())
                    .signedBy(approvedBy)
                    .signatureData(request.getSignatureData())
                    .signatureImagePath(request.getSignatureImagePath())
                    .ipAddress(request.getIpAddress())
                    .userAgent(request.getUserAgent())
                    .build();
            signatureRepository.save(signature);
        }

        // Update parcel status to ALLOCATED
        if (saved.getParcel() != null) {
            var parcel = saved.getParcel();
            parcel.setStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.ALLOCATED);
            parcel.setPto(saved);
            parcelRepository.save(parcel);
        }

        eventPublisher.publishEvent(new PTOApprovedEvent(
                this, saved.getId(), saved.getPtoNumber(),
                saved.getPtoHolderName(), approvedBy, saved.getApprovedAt()));

        log.info("PTO {} approved by {}", saved.getPtoNumber(), approvedBy);
        return toResponse(saved);
    }

    @Override
    public PTOResponse revokePTO(Long id, PTORevokeRequest request, String revokedBy) {
        var pto = ptoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PTO", id));

        if (!pto.canBeRevoked()) {
            throw new BusinessValidationException("PTO cannot be revoked in status: " + pto.getStatus().getDisplayName());
        }

        pto.setStatus(PTOStatus.REVOKED);
        pto.setRevokedBy(revokedBy);
        pto.setRevokedAt(LocalDateTime.now());
        pto.setRevokeReason(request.getReason());

        var saved = ptoRepository.save(pto);

        // Update parcel status back to AVAILABLE
        if (saved.getParcel() != null) {
            var parcel = saved.getParcel();
            parcel.setStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.AVAILABLE);
            parcel.setPto(null);
            parcelRepository.save(parcel);
        }

        eventPublisher.publishEvent(new PTORevokedEvent(
                this, saved.getId(), saved.getPtoNumber(),
                saved.getPtoHolderName(), revokedBy, request.getReason(), saved.getRevokedAt()));

        log.info("PTO {} revoked by {} — reason: {}", saved.getPtoNumber(), revokedBy, request.getReason());
        return toResponse(saved);
    }

    @Override
    public PTOResponse suspendPTO(Long id, String reason, String suspendedBy) {
        var pto = ptoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PTO", id));

        if (!pto.canBeSuspended()) {
            throw new BusinessValidationException("PTO cannot be suspended in status: " + pto.getStatus().getDisplayName());
        }

        pto.suspend(reason);
        var saved = ptoRepository.save(pto);

        // Update parcel status to RESERVED or keep as ALLOCATED but mark suspended
        if (saved.getParcel() != null) {
            var parcel = saved.getParcel();
            parcelRepository.save(parcel);
        }

        eventPublisher.publishEvent(new PTOSuspendedEvent(
                this, saved.getId(), saved.getPtoNumber(),
                saved.getPtoHolderName(), suspendedBy, reason, saved.getSuspendedAt()));

        log.info("PTO {} suspended by {} — reason: {}", saved.getPtoNumber(), suspendedBy, reason);
        return toResponse(saved);
    }

    @Override
    public PTOResponse reactivatePTO(Long id, String notes, String reactivatedBy) {
        var pto = ptoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PTO", id));

        if (pto.getStatus() != PTOStatus.SUSPENDED) {
            throw new BusinessValidationException("Only SUSPENDED PTOs can be reactivated");
        }

        // Check if the parcel has another active PTO
        if (pto.getParcel() != null && ptoRepository.existsByParcelIdAndStatus(pto.getParcel().getId(), PTOStatus.ACTIVE)) {
            throw new BusinessValidationException(
                    "Cannot reactivate: This parcel already has an ACTIVE PTO.");
        }

        pto.reactivate(notes);
        var saved = ptoRepository.save(pto);

        eventPublisher.publishEvent(new za.co.taloms.pto.domain.event.PTOStatusChangedEvent(
                this, saved.getId(), saved.getPtoNumber(),
                za.co.taloms.pto.domain.entity.PTOStatus.SUSPENDED,
                za.co.taloms.pto.domain.entity.PTOStatus.ACTIVE,
                reactivatedBy, java.time.LocalDateTime.now()));

        log.info("PTO {} reactivated by {}", saved.getPtoNumber(), reactivatedBy);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void reinstate(Long id, String reason) {
        PTO pto = ptoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PTO not found"));

        if (pto.getStatus() != PTOStatus.REVOKED) {
            throw new BusinessValidationException("Only revoked PTOs can be reinstated");
        }

        // Check if the parcel has another active PTO
        if (pto.getParcel() != null && ptoRepository.existsByParcelIdAndStatus(pto.getParcel().getId(), PTOStatus.ACTIVE)) {
            throw new BusinessValidationException(
                    "Cannot reinstate: This parcel already has an ACTIVE PTO.");
        }

        pto.reinstate(reason);
        ptoRepository.save(pto);

        // Update parcel status
        if (pto.getParcel() != null) {
            var parcel = pto.getParcel();
            parcel.setStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.ALLOCATED);
            parcel.setPto(pto);
            parcelRepository.save(parcel);
        }

        eventPublisher.publishEvent(new PTOReinstatedEvent(pto));
        log.info("PTO {} reinstated with reason: {}", pto.getPtoNumber(), reason);
    }

    // ... rest of the methods remain the same (findById, findAll, findByStatus, etc.)

    @Override
    @Transactional(readOnly = true)
    public PTOResponse findById(Long id) {
        return ptoRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("PTO", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PTOResponse findByPtoNumber(String ptoNumber) {
        return ptoRepository.findByPtoNumber(ptoNumber)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("PTO not found: " + ptoNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PTOResponse> findAll() {
        return ptoRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PTOResponse> findByStatus(PTOStatus status) {
        return ptoRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PTOResponse> findByAuthority(Long authorityId) {
        return ptoRepository.findByTraditionalAuthorityId(authorityId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PTOResponse> findByVillage(Long villageId) {
        return ptoRepository.findByVillageId(villageId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PTOResponse> findByParcel(Long parcelId) {
        return ptoRepository.findByParcelId(parcelId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PTOResponse> search(PTOSearchCriteria criteria) {
        return ptoRepository.search(
                criteria.getHolderName(),
                criteria.getIdNumber(),
                criteria.getPtoNumber(),
                criteria.getStatus(),
                criteria.getPurpose(),
                criteria.getVillageId(),
                criteria.getAuthorityId()
        ).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(PTOStatus status) {
        return ptoRepository.countByStatus(status);
    }

    @Override
    @Transactional
    public PTOResponse updatePTO(Long id, PTORequest request, String updatedBy) {
        var pto = ptoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PTO", id));

        if (pto.getStatus() != PTOStatus.PENDING && pto.getStatus() != PTOStatus.SUSPENDED) {
            throw new BusinessValidationException(
                    "PTO can only be edited when status is PENDING or SUSPENDED");
        }

        var parcel = parcelRepository.findById(request.getParcelId())
                .orElseThrow(() -> new ResourceNotFoundException("Parcel", request.getParcelId()));

        var authority = authorityRepository.findById(request.getTraditionalAuthorityId())
                .orElseThrow(() -> new ResourceNotFoundException("Traditional Authority", request.getTraditionalAuthorityId()));

        var village = villageRepository.findById(request.getVillageId())
                .orElseThrow(() -> new ResourceNotFoundException("Village", request.getVillageId()));

        if (!village.getTraditionalAuthority().getId().equals(authority.getId())) {
            throw new BusinessValidationException(
                    "Village does not belong to this Authority");
        }

        if (parcel.getVillage() == null || !parcel.getVillage().getId().equals(village.getId())) {
            throw new BusinessValidationException(
                    "Parcel does not belong to this Village");
        }

        if (!parcel.getStandNumber().equals(request.getStandNumber())) {
            throw new BusinessValidationException(
                    "Stand number does not match for this parcel");
        }

        if (!parcel.getId().equals(pto.getParcel() != null ? pto.getParcel().getId() : null) &&
                ptoRepository.existsByParcelIdAndStatus(parcel.getId(), PTOStatus.ACTIVE)) {
            throw new BusinessValidationException(
                    "Cannot update: The target parcel already has an ACTIVE PTO.");
        }

        pto.setPtoHolderName(request.getPtoHolderName());
        pto.setIdNumber(request.getIdNumber());
        pto.setContactPhone(request.getContactPhone());
        pto.setContactEmail(request.getContactEmail());
        pto.setPurpose(PTOPurpose.valueOf(request.getPurpose()));
        pto.setIssueDate(request.getIssueDate());
        pto.setExpiryDate(request.getExpiryDate());
        pto.setNotes(request.getNotes());
        pto.setVillage(village);
        pto.setTraditionalAuthority(authority);
        pto.setParcel(parcel);
        pto.setAllocatedBy(request.getAllocatedBy());
        pto.setAllocationDate(request.getAllocationDate());
        pto.setStandArea(request.getStandArea());
        pto.setSurveyReference(request.getSurveyReference());
        pto.setBoundaryDescription(request.getBoundaryDescription());
        pto.setAllocationFeeReceipt(request.getAllocationFeeReceipt());
        pto.setTaRecommendationRef(request.getTaRecommendationRef());
        if (request.getCommunityResolutionRequired() != null) {
            pto.setCommunityResolutionRequired(request.getCommunityResolutionRequired());
        }

        var saved = ptoRepository.save(pto);
        log.info("PTO {} updated by {}", saved.getPtoNumber(), updatedBy);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return ptoRepository.countAll();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByTraditionalAuthorityIdAndStatus(Long authorityId, PTOStatus status) {
        return ptoRepository.countByTraditionalAuthorityIdAndStatus(authorityId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByVillageIdAndStatus(Long villageId, PTOStatus status) {
        return ptoRepository.countByVillageIdAndStatus(villageId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByVillageId(Long villageId) {
        return ptoRepository.countByVillageId(villageId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByAuthorityIdAndIssueDateBetween(Long authorityId, java.time.LocalDate dateFrom, java.time.LocalDate dateTo) {
        return ptoRepository.countByAuthorityIdAndIssueDateBetween(authorityId, dateFrom, dateTo);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByVillageIdAndIssueDateBetween(Long villageId, java.time.LocalDate dateFrom, java.time.LocalDate dateTo) {
        return ptoRepository.countByVillageIdAndIssueDateBetween(villageId, dateFrom, dateTo);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByIssueDateBetween(java.time.LocalDate dateFrom, java.time.LocalDate dateTo) {
        return ptoRepository.countByIssueDateBetween(dateFrom, dateTo);
    }

    @Override
    @Transactional
    public void deletePTO(Long id, String deletedBy) {
        var pto = ptoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PTO", id));

        if (pto.isDeleted()) {
            throw new BusinessValidationException("PTO is already deleted");
        }

        pto.softDelete(deletedBy);
        ptoRepository.save(pto);

        eventPublisher.publishEvent(new PTODeletedEvent(
                this, pto.getId(), pto.getPtoNumber(),
                pto.getPtoHolderName(), deletedBy, pto.getDeletedAt(), pto.getRevokeReason()));

        log.info("PTO {} soft-deleted by {}", pto.getPtoNumber(), deletedBy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PTOResponse> findDeleted() {
        return ptoRepository.findDeleted().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
                .villageId(p.getVillage() != null ? p.getVillage().getId() : null)
                .villageName(p.getVillage() != null ? p.getVillage().getVillageName() : null)
                .traditionalAuthorityId(p.getTraditionalAuthority() != null ? p.getTraditionalAuthority().getId() : null)
                .authorityName(p.getTraditionalAuthority() != null ? p.getTraditionalAuthority().getAuthorityName() : null)
                .approvedBy(p.getApprovedBy())
                .approvedAt(p.getApprovedAt())
                .suspendedBy(p.getSuspendedBy())
                .suspendedAt(p.getSuspendedAt())
                .suspendReason(p.getSuspendReason())
                .reactivatedBy(p.getReactivatedBy())
                .reactivatedAt(p.getReactivatedAt())
                .reinstatedBy(p.getReinstatedBy())
                .reinstatedAt(p.getReinstatedAt())
                .reinstateReason(p.getReinstateReason())
                .revokedBy(p.getRevokedBy())
                .revokedAt(p.getRevokedAt())
                .revokeReason(p.getRevokeReason())
                .allocatedBy(p.getAllocatedBy())
                .allocationDate(p.getAllocationDate())
                .standArea(p.getStandArea())
                .surveyReference(p.getSurveyReference())
                .boundaryDescription(p.getBoundaryDescription())
                .allocationFeeReceipt(p.getAllocationFeeReceipt())
                .taRecommendationRef(p.getTaRecommendationRef())
                .communityResolutionRequired(p.getCommunityResolutionRequired())
                .createdBy(p.getCreatedBy())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .deletedAt(p.getDeletedAt())
                .deletedBy(p.getDeletedBy())
                .build();
    }
}

