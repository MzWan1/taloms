path = "src/main/java/za/co/taloms/document/domain/repository/DocumentRepositoryPort.java"
with open(path, "r") as f: c = f.read()
c = c.replace("List<Document> findByRelatedEntity(EntityType entityType, Long entityId);", "List<Document> findByRelatedEntity(EntityType entityType, Long entityId);\n    Page<Document> findByRelatedEntity(EntityType entityType, Long entityId, Pageable pageable);")
with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/document/application/service/DocumentService.java"
with open(path, "r") as f: c = f.read()
c = c.replace("List<DocumentResponse> findByRelatedEntity(EntityType entityType, Long entityId);", "List<DocumentResponse> findByRelatedEntity(EntityType entityType, Long entityId);\n    Page<DocumentResponse> findByRelatedEntity(EntityType entityType, Long entityId, Pageable pageable);")
with open(path, "w") as f: f.write(c)
