package za.co.taloms.pto.application.service;

import za.co.taloms.pto.application.dto.*;
import za.co.taloms.pto.domain.entity.PTOStatus;
import java.util.List;

public interface PTOService {
    PTOResponse createPTO(PTORequest request, String createdBy);
    PTOResponse updatePTO(Long id, PTORequest request, String updatedBy);
    PTOResponse findById(Long id);
    PTOResponse findByPtoNumber(String ptoNumber);
    List<PTOResponse> findAll();
    List<PTOResponse> findByStatus(PTOStatus status);
    List<PTOResponse> findByAuthority(Long authorityId);
    List<PTOResponse> findByVillage(Long villageId);
    List<PTOResponse> findByParcel(Long parcelId);
    List<PTOResponse> search(PTOSearchCriteria criteria);
    PTOResponse approvePTO(Long id, PTOApprovalRequest request, String approvedBy);
    PTOResponse revokePTO(Long id, PTORevokeRequest request, String revokedBy);
    PTOResponse suspendPTO(Long id, String reason, String suspendedBy);
    PTOResponse reactivatePTO(Long id, String notes, String reactivatedBy);
    long countByStatus(PTOStatus status);
    long countAll();
    long countByTraditionalAuthorityIdAndStatus(Long authorityId, PTOStatus status);
    long countByVillageIdAndStatus(Long villageId, PTOStatus status);
    long countByVillageId(Long villageId);
    long countByAuthorityIdAndIssueDateBetween(Long authorityId, java.time.LocalDate dateFrom, java.time.LocalDate dateTo);
    long countByVillageIdAndIssueDateBetween(Long villageId, java.time.LocalDate dateFrom, java.time.LocalDate dateTo);
    long countByIssueDateBetween(java.time.LocalDate dateFrom, java.time.LocalDate dateTo);
    void reinstate(Long id, String reason);
}