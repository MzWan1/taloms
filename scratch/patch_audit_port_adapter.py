path = "src/main/java/za/co/taloms/audit/domain/repository/AuditLogRepositoryPort.java"
with open(path, "r") as f: c = f.read()
if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.Optional;", "import java.util.Optional;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;")
c = c.replace("List<AuditLog> findAll();", "List<AuditLog> findAll();\n    Page<AuditLog> findAll(Pageable pageable);")
c = c.replace("List<AuditLog> findByEntity(String entityType, Long entityId);", "List<AuditLog> findByEntity(String entityType, Long entityId);\n    Page<AuditLog> findByEntity(String entityType, Long entityId, Pageable pageable);")
with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/audit/infrastructure/repository/AuditLogRepositoryAdapter.java"
with open(path, "r") as f: c = f.read()
if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.Optional;", "import java.util.Optional;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;")
c = c.replace("public List<AuditLog> findAll() {\n        return jpaRepository.findAllOrderByPerformedAtDesc();\n    }", "public List<AuditLog> findAll() {\n        return jpaRepository.findAllOrderByPerformedAtDesc();\n    }\n\n    @Override\n    public Page<AuditLog> findAll(Pageable pageable) {\n        return jpaRepository.findAllOrderByPerformedAtDesc(pageable);\n    }")
c = c.replace("public List<AuditLog> findByEntity(String entityType, Long entityId) {\n        return jpaRepository.findByEntityOrderByPerformedAtDesc(entityType, entityId);\n    }", "public List<AuditLog> findByEntity(String entityType, Long entityId) {\n        return jpaRepository.findByEntityOrderByPerformedAtDesc(entityType, entityId);\n    }\n\n    @Override\n    public Page<AuditLog> findByEntity(String entityType, Long entityId, Pageable pageable) {\n        return jpaRepository.findByEntityOrderByPerformedAtDesc(entityType, entityId, pageable);\n    }")
with open(path, "w") as f: f.write(c)
