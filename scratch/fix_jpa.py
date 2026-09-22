path = "src/main/java/za/co/taloms/pto/infrastructure/repository/PTOJpaRepository.java"
with open(path, "r") as f: c = f.read()

c = c.replace("List<PTO> findByTraditionalAuthorityId(@Param(\"authorityId\") Long authorityId, Pageable pageable);", "Page<PTO> findByTraditionalAuthorityId(@Param(\"authorityId\") Long authorityId, Pageable pageable);")

with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/pto/infrastructure/repository/PTORepositoryAdapter.java"
with open(path, "r") as f: c = f.read()
# Revert the false errors about lines 128 and 143.
# Wait, why did it complain about line 128? 
# "incompatible types: java.lang.Long cannot be converted to java.util.Set<java.lang.Long>"
# Line 128 is: return jpaRepository.countByVillageIdAndIssueDateBetween(villageId, dateFrom, dateTo);
# Did I accidentally change PTOJpaRepository to accept Set<Long> villageIds instead of villageId for countByVillageIdAndIssueDateBetween? No!
# Let's check PTOJpaRepository.
