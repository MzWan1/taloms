package za.co.taloms.parcel.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.parcel.domain.entity.ParcelBoundary;
import za.co.taloms.parcel.domain.repository.ParcelBoundaryRepositoryPort;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ParcelBoundaryRepositoryAdapter implements ParcelBoundaryRepositoryPort {

    private final ParcelBoundaryJpaRepository jpaRepository;

    @Override
    public ParcelBoundary save(ParcelBoundary boundary) {
        return jpaRepository.save(boundary);
    }

    @Override
    public List<ParcelBoundary> saveAll(List<ParcelBoundary> boundaries) {
        return jpaRepository.saveAll(boundaries);
    }

    @Override
    public List<ParcelBoundary> findByParcelId(Long parcelId) {
        return jpaRepository.findByParcelIdOrderBySequenceAsc(parcelId);
    }

    @Override
    public void deleteByParcelId(Long parcelId) {
        jpaRepository.deleteByParcelId(parcelId);
    }

    @Override
    public void deleteAll(List<ParcelBoundary> boundaries) {
        jpaRepository.deleteAll(boundaries);
    }
}