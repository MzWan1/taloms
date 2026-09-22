path = "src/main/java/za/co/taloms/parcel/application/service/ParcelServiceImpl.java"
with open(path, "r") as f: c = f.read()

additions = """
    @Override
    @Transactional(readOnly = true)
    public Page<ParcelResponse> searchParcels(String q, ParcelStatus status, Long villageId, java.util.Set<Long> allowedVillageIds, Pageable pageable) {
        return parcelRepository.searchParcels(q, status, villageId, allowedVillageIds, pageable).map(this::toResponse);
    }
"""

c = c.replace("public List<ParcelResponse> search(String query) {", additions + "\n    public List<ParcelResponse> search(String query) {")

with open(path, "w") as f: f.write(c)
