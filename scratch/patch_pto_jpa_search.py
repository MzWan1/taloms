path = "src/main/java/za/co/taloms/pto/infrastructure/repository/PTOJpaRepository.java"
with open(path, "r") as f: c = f.read()

# Change villageId to villageIds in search
c = c.replace("AND (:villageId IS NULL OR p.village.id = :villageId)", "AND (COALESCE(:villageIds, NULL) IS NULL OR p.village.id IN :villageIds)")
c = c.replace("@Param(\"villageId\") Long villageId,", "@Param(\"villageIds\") Set<Long> villageIds,")

with open(path, "w") as f: f.write(c)
