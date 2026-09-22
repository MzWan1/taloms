path = "src/main/java/za/co/taloms/pto/infrastructure/repository/PTORepositoryAdapter.java"
with open(path, "r") as f: c = f.read()
import re

old_search = r'    @Override\s+public Page<PTO> search\([^\{]+\{\s+return jpaRepository\.search\([^;]+;\s+\}'

new_search = """    @Override
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

c = re.sub(old_search, new_search, c)
with open(path, "w") as f: f.write(c)
