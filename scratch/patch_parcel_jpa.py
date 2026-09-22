path = "src/main/java/za/co/taloms/parcel/infrastructure/repository/ParcelJpaRepository.java"
with open(path, "r") as f: c = f.read()

if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.Optional;", "import java.util.Optional;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;")

additions = """
    @Query(\"""
        SELECT DISTINCT p FROM Parcel p
        LEFT JOIN p.village v
        WHERE (:q IS NULL OR LOWER(p.parcelNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.standNumber) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:status IS NULL OR p.status = :status)
          AND (:villageId IS NULL OR p.village.id = :villageId)
          AND (COALESCE(:allowedVillageIds, NULL) IS NULL OR p.village.id IN :allowedVillageIds)
        \""")
    Page<Parcel> searchParcels(
            @Param("q") String q,
            @Param("status") ParcelStatus status,
            @Param("villageId") Long villageId,
            @Param("allowedVillageIds") java.util.Set<Long> allowedVillageIds,
            Pageable pageable
    );
"""

c = c.replace("Optional<Parcel> findByParcelNumber(@Param(\"parcelNumber\") String parcelNumber);", "Optional<Parcel> findByParcelNumber(@Param(\"parcelNumber\") String parcelNumber);\n" + additions)

with open(path, "w") as f: f.write(c)
