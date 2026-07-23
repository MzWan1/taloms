package za.co.taloms.parcel.application.service;

import za.co.taloms.parcel.application.dto.BoundaryPointDto;
import za.co.taloms.parcel.application.dto.ParcelRequest;
import za.co.taloms.parcel.application.dto.ParcelResponse;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.parcel.domain.entity.ParcelType;
import java.util.List;

public interface ParcelService {
    ParcelResponse createParcel(ParcelRequest request, String createdBy);
    ParcelResponse updateParcel(Long id, ParcelRequest request, String updatedBy);
    ParcelResponse findById(Long id);
    ParcelResponse findByParcelNumber(String parcelNumber);
    List<ParcelResponse> findAll();
    List<ParcelResponse> findByVillage(Long villageId);
    List<ParcelResponse> findByStatus(ParcelStatus status);
    List<ParcelResponse> findByParcelType(ParcelType parcelType);
    List<ParcelResponse> findAvailable(Long villageId);
    List<ParcelResponse> findAllAvailable();  // NEW METHOD
    List<ParcelResponse> search(String query);
    ParcelResponse updateStatus(Long id, ParcelStatus status, String updatedBy);
    ParcelResponse allocateParcel(Long id, Long ptoId, String allocatedBy);
    void deleteParcel(Long id, String deletedBy);
    long countByStatus(ParcelStatus status);
    long countByVillage(Long villageId);
    long countAll();
    boolean isStandNumberUnique(String standNumber, Long villageId);
    Double calculateArea(List<BoundaryPointDto> boundaries);
}