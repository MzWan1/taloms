package za.co.taloms.parcel.domain.repository;

import za.co.taloms.parcel.domain.entity.Parcel;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ParcelRepositoryPort {
    Parcel save(Parcel parcel);
    Optional<Parcel> findById(Long id);
    Optional<Parcel> findByParcelNumber(String parcelNumber);
    Optional<Parcel> findByStandNumberAndVillageId(String standNumber, Long villageId);
    List<Parcel> findAll();
    List<Parcel> findByVillageId(Long villageId);
    List<Parcel> findByStatus(ParcelStatus status);
    List<Parcel> findByStatusAndVillageId(ParcelStatus status, Long villageId);
    List<Parcel> findAvailable(Long villageId);
    List<Parcel> findAllAvailable();
    List<Parcel> findByAuthorityId(Long authorityId);
    boolean existsByStandNumberAndVillageId(String standNumber, Long villageId);
    boolean existsByParcelNumber(String parcelNumber);
    long countByStatus(ParcelStatus status);
    long countByVillageId(Long villageId);
    long countByStatusAndVillageId(ParcelStatus status, Long villageId);
    long countAll();
    void deleteById(Long id);
    List<Parcel> findOverlappingParcels(Long parcelId, Double minLat, Double minLng, Double maxLat, Double maxLng);
    List<Object[]> findOverlappingParcelsWithGeometry(Long parcelId);
    boolean hasSelfIntersection(Long parcelId);
    List<Object[]> findParcelClusters(Long villageId, double epsMeters, int minPts);
    List<Object[]> findVoronoiCells(Long villageId);

    // Sync & Batch operations
    List<Parcel> findChangedSince(Instant since, int pageSize);
    List<Parcel> findByIds(Set<Long> ids);
    void saveAll(List<Parcel> parcels);
    void deleteAllByIds(Set<Long> ids);
}

