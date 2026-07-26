package za.co.taloms.parcel.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.taloms.parcel.domain.entity.Parcel;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import java.util.List;
import java.util.Optional;

public interface ParcelJpaRepository extends JpaRepository<Parcel, Long> {

    @Query("""
           SELECT DISTINCT p FROM Parcel p
           LEFT JOIN FETCH p.boundaries b
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority ta
           LEFT JOIN FETCH p.pto
           ORDER BY p.createdAt DESC
           """)
    List<Parcel> findAllOrderByCreatedAtDesc();

    @Query("""
           SELECT DISTINCT p FROM Parcel p
           LEFT JOIN FETCH p.boundaries b
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority ta
           LEFT JOIN FETCH p.pto
           WHERE p.parcelNumber = :parcelNumber
           """)
    Optional<Parcel> findByParcelNumber(@Param("parcelNumber") String parcelNumber);

    @Query("""
           SELECT DISTINCT p FROM Parcel p
           LEFT JOIN FETCH p.boundaries b
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority ta
           LEFT JOIN FETCH p.pto
           WHERE p.village.id = :villageId
           """)
    List<Parcel> findByVillageId(@Param("villageId") Long villageId);

    @Query("""
           SELECT DISTINCT p FROM Parcel p
           LEFT JOIN FETCH p.boundaries b
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority ta
           LEFT JOIN FETCH p.pto
           WHERE p.status = :status
           """)
    List<Parcel> findByStatus(@Param("status") ParcelStatus status);

    @Query("""
           SELECT DISTINCT p FROM Parcel p
           LEFT JOIN FETCH p.boundaries b
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority ta
           LEFT JOIN FETCH p.pto
           WHERE p.status = :status AND p.village.id = :villageId
           """)
    List<Parcel> findByStatusAndVillageId(@Param("status") ParcelStatus status,
                                          @Param("villageId") Long villageId);

    @Query("""
           SELECT DISTINCT p FROM Parcel p
           LEFT JOIN FETCH p.boundaries b
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority ta
           LEFT JOIN FETCH p.pto
           WHERE p.status = 'AVAILABLE' AND p.village.id = :villageId
           """)
    List<Parcel> findAvailableByVillageId(@Param("villageId") Long villageId);

    @Query("""
           SELECT DISTINCT p FROM Parcel p
           LEFT JOIN FETCH p.boundaries b
           LEFT JOIN FETCH p.village v
           LEFT JOIN FETCH v.traditionalAuthority ta
           LEFT JOIN FETCH p.pto
           WHERE p.id = :id
           """)
    Optional<Parcel> findByIdWithRelations(@Param("id") Long id);

    Optional<Parcel> findByStandNumberAndVillageId(String standNumber, Long villageId);

    boolean existsByStandNumberAndVillageId(String standNumber, Long villageId);

    boolean existsByParcelNumber(String parcelNumber);

    long countByStatus(ParcelStatus status);

    long countByVillageId(Long villageId);

    long countByStatusAndVillageId(ParcelStatus status, Long villageId);

    @Query(value = """
            SELECT p.* FROM parcels p
            WHERE p.id != :parcelId
              AND p.geometry IS NOT NULL
              AND ST_Intersects(
                  p.geometry,
                  ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
              )
            """, nativeQuery = true)
    List<Parcel> findOverlappingParcels(@Param("parcelId") Long parcelId,
                                        @Param("minLat") Double minLat,
                                        @Param("minLng") Double minLng,
                                        @Param("maxLat") Double maxLat,
                                        @Param("maxLng") Double maxLng);

    @Query(value = """
            SELECT p.id, p.parcel_number, p.stand_number, p.village_id,
                   ST_AsText(p.geometry) as geom_wkt
            FROM parcels p
            WHERE p.id != :parcelId
              AND p.geometry IS NOT NULL
              AND ST_Intersects(
                  p.geometry,
                  (SELECT geometry FROM parcels WHERE id = :parcelId)
              )
            """, nativeQuery = true)
    List<Object[]> findOverlappingParcelsWithGeometry(@Param("parcelId") Long parcelId);

    @Query(value = """
            SELECT NOT ST_IsValid(p.geometry)
            FROM parcels p
            WHERE p.id = :parcelId
              AND p.geometry IS NOT NULL
            """, nativeQuery = true)
    boolean hasSelfIntersection(@Param("parcelId") Long parcelId);

    @Query(value = """
            SELECT p.id, p.parcel_number, p.stand_number,
                   ST_ClusterDBSCAN(ST_Centroid(p.geometry), eps := :epsMeters, minpoints := :minPts)
                       OVER () AS cluster_id
            FROM parcels p
            WHERE p.village_id = :villageId
              AND p.geometry IS NOT NULL
              AND ST_IsValid(p.geometry)
            GROUP BY p.id, p.parcel_number, p.stand_number,
                     ST_ClusterDBSCAN(ST_Centroid(p.geometry), eps := :epsMeters, minpoints := :minPts)
            ORDER BY cluster_id
            """, nativeQuery = true)
    List<Object[]> findParcelClusters(@Param("villageId") Long villageId,
                                       @Param("epsMeters") double epsMeters,
                                       @Param("minPts") int minPts);

    @Query(value = """
            SELECT ST_VoronoiPolygons(
                ST_Collect(ST_Centroid(p.geometry)),
                0.0001
            ).geom as voronoi_geom
            FROM parcels p
            WHERE p.village_id = :villageId
              AND p.geometry IS NOT NULL
              AND ST_IsValid(p.geometry)
            """, nativeQuery = true)
    List<Object[]> findVoronoiCells(@Param("villageId") Long villageId);
}

