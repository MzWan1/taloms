path = "src/main/java/za/co/taloms/document/infrastructure/repository/DocumentRepositoryAdapter.java"
with open(path, "r") as f: c = f.read()

c = c.replace("public List<Document> findByRelatedEntity(EntityType entityType, Long entityId) {\n        return jpaRepository.findByRelatedEntityTypeAndRelatedEntityId(entityType, entityId);\n    }", "public List<Document> findByRelatedEntity(EntityType entityType, Long entityId) {\n        return jpaRepository.findByRelatedEntityTypeAndRelatedEntityId(entityType, entityId);\n    }\n\n    @Override\n    public Page<Document> findByRelatedEntity(EntityType entityType, Long entityId, Pageable pageable) {\n        return jpaRepository.findByRelatedEntityOrderByUploadedAtDesc(entityType, entityId, pageable);\n    }")

with open(path, "w") as f: f.write(c)
