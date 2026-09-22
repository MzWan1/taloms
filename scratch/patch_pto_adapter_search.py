path = "src/main/java/za/co/taloms/pto/infrastructure/repository/PTORepositoryAdapter.java"
with open(path, "r") as f: c = f.read()

c = c.replace("PTOPurpose purpose, Long villageId, Long authorityId, Pageable pageable) {\n        return jpaRepository.search(holderName, idNumber, ptoNumber, status, purpose, villageId, authorityId, pageable);", "PTOPurpose purpose, Set<Long> villageIds, Long authorityId, Pageable pageable) {\n        return jpaRepository.search(holderName, idNumber, ptoNumber, status, purpose, (villageIds != null && villageIds.isEmpty()) ? null : villageIds, authorityId, pageable);")

c = c.replace("PTOPurpose purpose, Long villageId, Long authorityId) {\n        return jpaRepository.search(holderName, idNumber, ptoNumber, status, purpose, villageId, authorityId);", "PTOPurpose purpose, Set<Long> villageIds, Long authorityId) {\n        return jpaRepository.search(holderName, idNumber, ptoNumber, status, purpose, (villageIds != null && villageIds.isEmpty()) ? null : villageIds, authorityId);")

with open(path, "w") as f: f.write(c)
