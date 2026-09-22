path = "src/main/java/za/co/taloms/pto/infrastructure/repository/PTOJpaRepository.java"
with open(path, "r") as f: c = f.read()

c = c.replace("long countByVillageIdAndIssueDateBetween(@Param(\"villageIds\") Set<Long> villageIds, @Param(\"dateFrom\") java.time.LocalDate dateFrom, @Param(\"dateTo\") java.time.LocalDate dateTo);", "long countByVillageIdAndIssueDateBetween(@Param(\"villageId\") Long villageId, @Param(\"dateFrom\") java.time.LocalDate dateFrom, @Param(\"dateTo\") java.time.LocalDate dateTo);")

c = c.replace("boolean existsByIdNumberAndVillageIdAndStatus(@Param(\"idNumber\") String idNumber, @Param(\"villageIds\") Set<Long> villageIds, @Param(\"status\") PTOStatus status);", "boolean existsByIdNumberAndVillageIdAndStatus(@Param(\"idNumber\") String idNumber, @Param(\"villageId\") Long villageId, @Param(\"status\") PTOStatus status);")

with open(path, "w") as f: f.write(c)
