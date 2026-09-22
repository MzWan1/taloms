path = "src/main/java/za/co/taloms/audit/application/service/AuditService.java"
with open(path, "r") as f: c = f.read()
if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.List;", "import java.util.List;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;")
c = c.replace("List<AuditLogResponse> findAll();", "List<AuditLogResponse> findAll();\n    Page<AuditLogResponse> findAll(Pageable pageable);")
c = c.replace("List<AuditLogResponse> findByEntity(String entityType, Long entityId);", "List<AuditLogResponse> findByEntity(String entityType, Long entityId);\n    Page<AuditLogResponse> findByEntity(String entityType, Long entityId, Pageable pageable);")
with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/audit/application/service/AuditServiceImpl.java"
with open(path, "r") as f: c = f.read()
if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.stream.Collectors;", "import java.util.stream.Collectors;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;")
c = c.replace("public List<AuditLogResponse> findAll() {\n        return auditRepository.findAll().stream()\n                .map(this::toResponse)\n                .collect(Collectors.toList());\n    }", "public List<AuditLogResponse> findAll() {\n        return auditRepository.findAll().stream()\n                .map(this::toResponse)\n                .collect(Collectors.toList());\n    }\n\n    @Override\n    @Transactional(readOnly = true)\n    public Page<AuditLogResponse> findAll(Pageable pageable) {\n        return auditRepository.findAll(pageable).map(this::toResponse);\n    }")
c = c.replace("public List<AuditLogResponse> findByEntity(String entityType, Long entityId) {\n        return auditRepository.findByEntity(entityType, entityId).stream()\n                .map(this::toResponse)\n                .collect(Collectors.toList());\n    }", "public List<AuditLogResponse> findByEntity(String entityType, Long entityId) {\n        return auditRepository.findByEntity(entityType, entityId).stream()\n                .map(this::toResponse)\n                .collect(Collectors.toList());\n    }\n\n    @Override\n    @Transactional(readOnly = true)\n    public Page<AuditLogResponse> findByEntity(String entityType, Long entityId, Pageable pageable) {\n        return auditRepository.findByEntity(entityType, entityId, pageable).map(this::toResponse);\n    }")
with open(path, "w") as f: f.write(c)
