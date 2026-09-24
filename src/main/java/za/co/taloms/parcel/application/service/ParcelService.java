package za.co.taloms.parcel.application.service;

import za.co.taloms.parcel.application.dto.BoundaryPointDto;
import za.co.taloms.parcel.application.dto.ParcelRequest;
import za.co.taloms.parcel.application.dto.ParcelResponse;
import za.co.taloms.parcel.application.dto.ParcelSyncDto;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Set;
import java.util.Set;

public interface ParcelService {
    ParcelResponse createParcel(ParcelRequest request, String createdBy);
    ParcelResponse updateParcel(Long id, ParcelRequest request, String updatedBy);
    ParcelResponse findById(Long id);
    ParcelResponse findByParcelNumber(String parcelNumber);
    List<ParcelResponse> findAll();
    List<ParcelResponse> findByVillage(Long villageId);
    List<ParcelResponse> findByStatus(ParcelStatus status);
    List<ParcelResponse> findAvailable(Long villageId);
    List<ParcelResponse> findAllAvailable();
    List<ParcelResponse> findByAuthorityId(Long authorityId);
    List<ParcelResponse> search(String query);
    Page<ParcelResponse> searchParcels(String q, ParcelStatus status, Long villageId, Set<Long> allowedVillageIds, Pageable pageable);
    ParcelResponse updateStatus(Long id, ParcelStatus status, String updatedBy);
    ParcelResponse allocateParcel(Long id, Long ptoId, String allocatedBy);
    void deleteParcel(Long id, String deletedBy);
    long countByStatus(ParcelStatus status);
    long countByVillage(Long villageId);
    long countByStatusAndVillage(ParcelStatus status, Long villageId);
    long countAll();
    boolean isStandNumberUnique(String standNumber, Long villageId);
    Double calculateArea(List<BoundaryPointDto> boundaries);

    // Sync operations
    List<ParcelSyncDto> findChangedSince(Instant since, int pageSize);
    List<ParcelSyncDto> findByIds(Set<Long> ids);
    void saveAll(List<ParcelSyncDto> parcels, String savedBy);
    void deleteAllByIds(Set<Long> ids, String deletedBy);

    // Batch operations
    List<ParcelResponse> createBatch(List<ParcelRequest> requests, String createdBy);
    void deleteBatch(Set<Long> ids, String deletedBy);
}

