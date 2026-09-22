path = "src/main/java/za/co/taloms/parcel/domain/repository/ParcelRepositoryPort.java"
with open(path, "r") as f: c = f.read()
if "Page<Parcel> searchParcels" not in c:
    c = c.replace("List<Parcel> findAll();", "List<Parcel> findAll();\n    Page<Parcel> searchParcels(String q, ParcelStatus status, Long villageId, java.util.Set<Long> allowedVillageIds, Pageable pageable);")
with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/parcel/infrastructure/repository/ParcelRepositoryAdapter.java"
with open(path, "r") as f: c = f.read()
if "Page<Parcel> searchParcels" not in c:
    additions_adapter = """
    @Override
    public Page<Parcel> searchParcels(String q, ParcelStatus status, Long villageId, java.util.Set<Long> allowedVillageIds, Pageable pageable) {
        return jpaRepository.searchParcels(q, status, villageId, (allowedVillageIds != null && allowedVillageIds.isEmpty()) ? null : allowedVillageIds, pageable);
    }
"""
    c = c.replace("public List<Parcel> findAll() {\n        return jpaRepository.findAllOrderByCreatedAtDesc();\n    }", "public List<Parcel> findAll() {\n        return jpaRepository.findAllOrderByCreatedAtDesc();\n    }\n" + additions_adapter)
with open(path, "w") as f: f.write(c)
