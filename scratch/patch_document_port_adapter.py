path = "src/main/java/za/co/taloms/document/domain/repository/DocumentRepositoryPort.java"
with open(path, "r") as f: c = f.read()
if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.Optional;", "import java.util.Optional;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;")
c = c.replace("List<Document> findAll();", "List<Document> findAll();\n    Page<Document> findAll(Pageable pageable);")
c = c.replace("List<Document> findByRelatedEntity(za.co.taloms.document.domain.entity.EntityType entityType, Long entityId);", "List<Document> findByRelatedEntity(za.co.taloms.document.domain.entity.EntityType entityType, Long entityId);\n    Page<Document> findByRelatedEntity(za.co.taloms.document.domain.entity.EntityType entityType, Long entityId, Pageable pageable);")
with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/document/infrastructure/repository/DocumentRepositoryAdapter.java"
with open(path, "r") as f: c = f.read()
if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.Optional;", "import java.util.Optional;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;")
c = c.replace("public List<Document> findAll() {\n        return jpaRepository.findAllOrderByUploadedAtDesc();\n    }", "public List<Document> findAll() {\n        return jpaRepository.findAllOrderByUploadedAtDesc();\n    }\n\n    @Override\n    public Page<Document> findAll(Pageable pageable) {\n        return jpaRepository.findAllOrderByUploadedAtDesc(pageable);\n    }")
c = c.replace("public List<Document> findByRelatedEntity(za.co.taloms.document.domain.entity.EntityType entityType, Long entityId) {\n        return jpaRepository.findByRelatedEntityOrderByUploadedAtDesc(entityType, entityId);\n    }", "public List<Document> findByRelatedEntity(za.co.taloms.document.domain.entity.EntityType entityType, Long entityId) {\n        return jpaRepository.findByRelatedEntityOrderByUploadedAtDesc(entityType, entityId);\n    }\n\n    @Override\n    public Page<Document> findByRelatedEntity(za.co.taloms.document.domain.entity.EntityType entityType, Long entityId, Pageable pageable) {\n        return jpaRepository.findByRelatedEntityOrderByUploadedAtDesc(entityType, entityId, pageable);\n    }")
with open(path, "w") as f: f.write(c)
