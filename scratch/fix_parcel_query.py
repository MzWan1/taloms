path = "src/main/java/za/co/taloms/parcel/infrastructure/repository/ParcelJpaRepository.java"
with open(path, "r") as f: c = f.read()

import re

old_query = r"""    @Query\(\"""
        SELECT DISTINCT p FROM Parcel p
        LEFT JOIN p.village v
        WHERE \(:q IS NULL OR LOWER\(p.parcelNumber\) LIKE LOWER\(CONCAT\('%', :q, '%'\)\) OR LOWER\(p.standNumber\) LIKE LOWER\(CONCAT\('%', :q, '%'\)\)\)
          AND \(:status IS NULL OR p.status = :status\)
          AND \(:villageId IS NULL OR p.village.id = :villageId\)
          AND \(COALESCE\(:allowedVillageIds, NULL\) IS NULL OR p.village.id IN :allowedVillageIds\)
        \"""\)
    Page<Parcel> searchParcels\(
            @Param\("q"\) String q,
            @Param\("status"\) ParcelStatus status,
            @Param\("villageId"\) Long villageId,
            @Param\("allowedVillageIds"\) java.util.Set<Long> allowedVillageIds,
            Pageable pageable
    \);"""

new_query = """    @Query(\"\"\"
        SELECT DISTINCT p FROM Parcel p
        LEFT JOIN p.village v
        WHERE (:filterByQ = false OR LOWER(p.parcelNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.standNumber) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:filterByStatus = false OR p.status = :status)
          AND (:filterByVillage = false OR p.village.id = :villageId)
          AND (:filterByAllowedVillages = false OR p.village.id IN :allowedVillageIds)
        \"\"\")
    Page<Parcel> searchParcels(
            @Param("q") String q,
            @Param("filterByQ") boolean filterByQ,
            @Param("status") ParcelStatus status,
            @Param("filterByStatus") boolean filterByStatus,
            @Param("villageId") Long villageId,
            @Param("filterByVillage") boolean filterByVillage,
            @Param("allowedVillageIds") java.util.Set<Long> allowedVillageIds,
            @Param("filterByAllowedVillages") boolean filterByAllowedVillages,
            Pageable pageable
    );"""

c = re.sub(old_query, new_query, c, flags=re.MULTILINE)
with open(path, "w") as f: f.write(c)


path_adapter = "src/main/java/za/co/taloms/parcel/infrastructure/repository/ParcelRepositoryAdapter.java"
with open(path_adapter, "r") as f: c = f.read()

old_adapter = """    @Override
    public Page<Parcel> searchParcels(String q, ParcelStatus status, Long villageId, java.util.Set<Long> allowedVillageIds, Pageable pageable) {
        return jpaRepository.searchParcels(q, status, villageId, (allowedVillageIds != null && allowedVillageIds.isEmpty()) ? null : allowedVillageIds, pageable);
    }"""

new_adapter = """    @Override
    public Page<Parcel> searchParcels(String q, ParcelStatus status, Long villageId, java.util.Set<Long> allowedVillageIds, Pageable pageable) {
        boolean filterByQ = (q != null && !q.trim().isEmpty());
        boolean filterByStatus = (status != null);
        boolean filterByVillage = (villageId != null);
        boolean filterByAllowedVillages = (allowedVillageIds != null && !allowedVillageIds.isEmpty());
        
        // Provide dummy values for null collections to avoid Hibernate errors on empty collections in some dialects
        java.util.Set<Long> safeVillageIds = filterByAllowedVillages ? allowedVillageIds : java.util.Set.of(-1L);
        String safeQ = filterByQ ? q : "";
        
        return jpaRepository.searchParcels(safeQ, filterByQ, status, filterByStatus, villageId, filterByVillage, safeVillageIds, filterByAllowedVillages, pageable);
    }"""

c = c.replace(old_adapter, new_adapter)
with open(path_adapter, "w") as f: f.write(c)

