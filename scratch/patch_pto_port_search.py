path = "src/main/java/za/co/taloms/pto/domain/repository/PTORepositoryPort.java"
with open(path, "r") as f: c = f.read()

c = c.replace("PTOPurpose purpose, Long villageId, Long authorityId, Pageable pageable);", "PTOPurpose purpose, Set<Long> villageIds, Long authorityId, Pageable pageable);")
c = c.replace("PTOPurpose purpose, Long villageId, Long authorityId);", "PTOPurpose purpose, Set<Long> villageIds, Long authorityId);")

with open(path, "w") as f: f.write(c)
