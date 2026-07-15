package za.co.taloms.parcel.domain.repository;

import za.co.taloms.parcel.domain.entity.ParcelBoundary;
import java.util.List;

public interface ParcelBoundaryRepositoryPort {
    ParcelBoundary save(ParcelBoundary boundary);
    List<ParcelBoundary> saveAll(List<ParcelBoundary> boundaries);
    List<ParcelBoundary> findByParcelId(Long parcelId);
    void deleteByParcelId(Long parcelId);
    void deleteAll(List<ParcelBoundary> boundaries);
}