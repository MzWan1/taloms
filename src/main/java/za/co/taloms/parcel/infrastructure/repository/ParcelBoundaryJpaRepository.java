package za.co.taloms.parcel.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.taloms.parcel.domain.entity.ParcelBoundary;
import java.util.List;

public interface ParcelBoundaryJpaRepository extends JpaRepository<ParcelBoundary, Long> {

    List<ParcelBoundary> findByParcelIdOrderBySequenceAsc(Long parcelId);

    @Modifying
    @Query("DELETE FROM ParcelBoundary pb WHERE pb.parcel.id = :parcelId")
    void deleteByParcelId(@Param("parcelId") Long parcelId);
}