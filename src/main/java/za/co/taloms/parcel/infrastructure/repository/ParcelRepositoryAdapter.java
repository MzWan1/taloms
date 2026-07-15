package za.co.taloms.parcel.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.parcel.domain.entity.Parcel;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.parcel.domain.entity.ParcelType;
import za.co.taloms.parcel.domain.repository.ParcelRepositoryPort;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ParcelRepositoryAdapter implements ParcelRepositoryPort {

    private final ParcelJpaRepository jpaRepository;

    @Override
    public Parcel save(Parcel parcel) {
        return jpaRepository.save(parcel);
    }

    @Override
    public Optional<Parcel> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Parcel> findByParcelNumber(String parcelNumber) {
        return jpaRepository.findByParcelNumber(parcelNumber);
    }

    @Override
    public Optional<Parcel> findByStandNumberAndVillageId(String standNumber, Long villageId) {
        return jpaRepository.findByStandNumberAndVillageId(standNumber, villageId);
    }

    @Override
    public List<Parcel> findAll() {
        return jpaRepository.findAllOrderByCreatedAtDesc();
    }

    @Override
    public List<Parcel> findByVillageId(Long villageId) {
        return jpaRepository.findByVillageId(villageId);
    }

    @Override
    public List<Parcel> findByStatus(ParcelStatus status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public List<Parcel> findByParcelType(ParcelType parcelType) {
        return jpaRepository.findByParcelType(parcelType);
    }

    @Override
    public List<Parcel> findByStatusAndVillageId(ParcelStatus status, Long villageId) {
        return jpaRepository.findByStatusAndVillageId(status, villageId);
    }

    @Override
    public List<Parcel> findAvailable(Long villageId) {
        return jpaRepository.findAvailableByVillageId(villageId);
    }

    @Override
    public boolean existsByStandNumberAndVillageId(String standNumber, Long villageId) {
        return jpaRepository.existsByStandNumberAndVillageId(standNumber, villageId);
    }

    @Override
    public boolean existsByParcelNumber(String parcelNumber) {
        return jpaRepository.existsByParcelNumber(parcelNumber);
    }

    @Override
    public long countByStatus(ParcelStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public long countByVillageId(Long villageId) {
        return jpaRepository.countByVillageId(villageId);
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}