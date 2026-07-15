package za.co.taloms.parcel.domain.repository;

import za.co.taloms.parcel.domain.entity.Parcel;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.parcel.domain.entity.ParcelType;
import java.util.List;
import java.util.Optional;

public interface ParcelRepositoryPort {
    Parcel save(Parcel parcel);
    Optional<Parcel> findById(Long id);
    Optional<Parcel> findByParcelNumber(String parcelNumber);
    Optional<Parcel> findByStandNumberAndVillageId(String standNumber, Long villageId);
    List<Parcel> findAll();
    List<Parcel> findByVillageId(Long villageId);
    List<Parcel> findByStatus(ParcelStatus status);
    List<Parcel> findByParcelType(ParcelType parcelType);
    List<Parcel> findByStatusAndVillageId(ParcelStatus status, Long villageId);
    List<Parcel> findAvailable(Long villageId);
    boolean existsByStandNumberAndVillageId(String standNumber, Long villageId);
    boolean existsByParcelNumber(String parcelNumber);
    long countByStatus(ParcelStatus status);
    long countByVillageId(Long villageId);
    long countAll();
    void deleteById(Long id);
}