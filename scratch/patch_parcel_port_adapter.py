path = "src/main/java/za/co/taloms/parcel/domain/repository/ParcelRepositoryPort.java"
with open(path, "r") as f: c = f.read()
if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.Optional;", "import java.util.Optional;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;\nimport java.util.Set;\nimport za.co.taloms.parcel.domain.entity.ParcelStatus;")
c = c.replace("List<Parcel> search(String query);", "List<Parcel> search(String query);\n    Page<Parcel> searchParcels(String q, ParcelStatus status, Long villageId, Set<Long> allowedVillageIds, Pageable pageable);")
with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/parcel/infrastructure/repository/ParcelRepositoryAdapter.java"
with open(path, "r") as f: c = f.read()
if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.Optional;", "import java.util.Optional;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;\nimport java.util.Set;")
c = c.replace("public List<Parcel> search(String query) {\n        return jpaRepository.search(query);\n    }", "public List<Parcel> search(String query) {\n        return jpaRepository.search(query);\n    }\n\n    @Override\n    public Page<Parcel> searchParcels(String q, ParcelStatus status, Long villageId, Set<Long> allowedVillageIds, Pageable pageable) {\n        return jpaRepository.searchParcels(q, status, villageId, (allowedVillageIds != null && allowedVillageIds.isEmpty()) ? null : allowedVillageIds, pageable);\n    }")
with open(path, "w") as f: f.write(c)
