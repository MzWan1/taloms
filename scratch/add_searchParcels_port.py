path = "src/main/java/za/co/taloms/parcel/domain/repository/ParcelRepositoryPort.java"
with open(path, "r") as f: c = f.read()

additions = """
    Page<Parcel> searchParcels(String q, ParcelStatus status, Long villageId, java.util.Set<Long> allowedVillageIds, Pageable pageable);
"""

c = c.replace("List<Parcel> findAll();", "List<Parcel> findAll();\n" + additions)
with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/parcel/infrastructure/repository/ParcelRepositoryAdapter.java"
with open(path, "r") as f: c = f.read()

additions_adapter = """
    @Override
    public Page<Parcel> searchParcels(String q, ParcelStatus status, Long villageId, java.util.Set<Long> allowedVillageIds, Pageable pageable) {
        return jpaRepository.searchParcels(q, status, villageId, (allowedVillageIds != null && allowedVillageIds.isEmpty()) ? null : allowedVillageIds, pageable);
    }
"""

c = c.replace("public List<Parcel> findAll() {\n        return jpaRepository.findAll();\n    }", "public List<Parcel> findAll() {\n        return jpaRepository.findAll();\n    }\n" + additions_adapter)
with open(path, "w") as f: f.write(c)
