path = "src/main/java/za/co/taloms/pto/infrastructure/repository/PTOJpaRepository.java"
with open(path, "r") as f: c = f.read()

import re

old_query = r"""    @Query\(\"""
            SELECT p FROM PTO p WHERE
            \(CAST\(:holderName AS String\) IS NULL
                OR LOWER\(p.ptoHolderName\) LIKE LOWER\(CONCAT\('%', CAST\(:holderName AS String\), '%'\)\)\)
            AND \(CAST\(:idNumber AS String\) IS NULL
                OR p.idNumber LIKE CONCAT\('%', CAST\(:idNumber AS String\), '%'\)\)
            AND \(CAST\(:ptoNumber AS String\) IS NULL
                OR LOWER\(p.ptoNumber\) LIKE LOWER\(CONCAT\('%', CAST\(:ptoNumber AS String\), '%'\)\)\)
            AND \(:status IS NULL OR p.status = :status\)
            AND \(:purpose IS NULL OR p.purpose = :purpose\)
            AND \(COALESCE\(:villageIds, NULL\) IS NULL OR p.village.id IN :villageIds\)
            AND \(:authorityId IS NULL OR p.traditionalAuthority.id = :authorityId\)
            AND p.deletedAt IS NULL
            ORDER BY p.createdAt DESC
            \"""\)
    Page<PTO> search\(@Param\("holderName"\) String holderName,
                     @Param\("idNumber"\) String idNumber,
                     @Param\("ptoNumber"\) String ptoNumber,
                     @Param\("status"\) PTOStatus status,
                     @Param\("purpose"\) za.co.taloms.pto.domain.entity.PTOPurpose purpose,
                     @Param\("villageIds"\) Set<Long> villageIds,
                     @Param\("authorityId"\) Long authorityId, Pageable pageable\);"""

new_query = """    @Query(\"\"\"
            SELECT p FROM PTO p WHERE
            (:filterByHolder = false OR LOWER(p.ptoHolderName) LIKE LOWER(CONCAT('%', :holderName, '%')))
            AND (:filterById = false OR p.idNumber LIKE CONCAT('%', :idNumber, '%'))
            AND (:filterByPto = false OR LOWER(p.ptoNumber) LIKE LOWER(CONCAT('%', :ptoNumber, '%')))
            AND (:filterByStatus = false OR p.status = :status)
            AND (:filterByPurpose = false OR p.purpose = :purpose)
            AND (:filterByVillages = false OR p.village.id IN :villageIds)
            AND (:filterByAuthority = false OR p.traditionalAuthority.id = :authorityId)
            AND p.deletedAt IS NULL
            ORDER BY p.createdAt DESC
            \"\"\")
    Page<PTO> search(@Param("holderName") String holderName, @Param("filterByHolder") boolean filterByHolder,
                     @Param("idNumber") String idNumber, @Param("filterById") boolean filterById,
                     @Param("ptoNumber") String ptoNumber, @Param("filterByPto") boolean filterByPto,
                     @Param("status") PTOStatus status, @Param("filterByStatus") boolean filterByStatus,
                     @Param("purpose") za.co.taloms.pto.domain.entity.PTOPurpose purpose, @Param("filterByPurpose") boolean filterByPurpose,
                     @Param("villageIds") Set<Long> villageIds, @Param("filterByVillages") boolean filterByVillages,
                     @Param("authorityId") Long authorityId, @Param("filterByAuthority") boolean filterByAuthority,
                     Pageable pageable);"""

c = re.sub(old_query, new_query, c, flags=re.MULTILINE)
with open(path, "w") as f: f.write(c)


path_adapter = "src/main/java/za/co/taloms/pto/infrastructure/repository/PTORepositoryAdapter.java"
with open(path_adapter, "r") as f: c = f.read()

old_adapter = """    @Override
    public Page<PTO> search(String holderName, String idNumber, String ptoNumber, PTOStatus status, PTOPurpose purpose, Set<Long> villageIds, Long authorityId, Pageable pageable) {
        return jpaRepository.search(holderName, idNumber, ptoNumber, status, purpose, villageIds, authorityId, pageable);
    }"""

new_adapter = """    @Override
    public Page<PTO> search(String holderName, String idNumber, String ptoNumber, PTOStatus status, PTOPurpose purpose, Set<Long> villageIds, Long authorityId, Pageable pageable) {
        boolean filterByHolder = holderName != null && !holderName.trim().isEmpty();
        boolean filterById = idNumber != null && !idNumber.trim().isEmpty();
        boolean filterByPto = ptoNumber != null && !ptoNumber.trim().isEmpty();
        boolean filterByStatus = status != null;
        boolean filterByPurpose = purpose != null;
        boolean filterByVillages = villageIds != null && !villageIds.isEmpty();
        boolean filterByAuthority = authorityId != null;

        String safeHolder = filterByHolder ? holderName : "";
        String safeId = filterById ? idNumber : "";
        String safePto = filterByPto ? ptoNumber : "";
        Set<Long> safeVillageIds = filterByVillages ? villageIds : Set.of(-1L);

        return jpaRepository.search(
                safeHolder, filterByHolder,
                safeId, filterById,
                safePto, filterByPto,
                status, filterByStatus,
                purpose, filterByPurpose,
                safeVillageIds, filterByVillages,
                authorityId, filterByAuthority,
                pageable
        );
    }"""

c = c.replace(old_adapter, new_adapter)
with open(path_adapter, "w") as f: f.write(c)

