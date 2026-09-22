path = "src/main/java/za/co/taloms/document/application/service/DocumentService.java"
with open(path, "r") as f: c = f.read()
if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.List;", "import java.util.List;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;")
c = c.replace("List<DocumentResponse> findAll();", "List<DocumentResponse> findAll();\n    Page<DocumentResponse> findAll(Pageable pageable);")
c = c.replace("List<DocumentResponse> findByRelatedEntity(za.co.taloms.document.domain.entity.EntityType entityType, Long entityId);", "List<DocumentResponse> findByRelatedEntity(za.co.taloms.document.domain.entity.EntityType entityType, Long entityId);\n    Page<DocumentResponse> findByRelatedEntity(za.co.taloms.document.domain.entity.EntityType entityType, Long entityId, Pageable pageable);")
with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/document/application/service/DocumentServiceImpl.java"
with open(path, "r") as f: c = f.read()
if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.stream.Collectors;", "import java.util.stream.Collectors;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;")
c = c.replace("public List<DocumentResponse> findAll() {\n        return documentRepository.findAll().stream()\n                .map(this::toResponse)\n                .collect(Collectors.toList());\n    }", "public List<DocumentResponse> findAll() {\n        return documentRepository.findAll().stream()\n                .map(this::toResponse)\n                .collect(Collectors.toList());\n    }\n\n    @Override\n    @Transactional(readOnly = true)\n    public Page<DocumentResponse> findAll(Pageable pageable) {\n        return documentRepository.findAll(pageable).map(this::toResponse);\n    }")
c = c.replace("public List<DocumentResponse> findByRelatedEntity(EntityType entityType, Long entityId) {\n        return documentRepository.findByRelatedEntity(entityType, entityId).stream()\n                .map(this::toResponse)\n                .collect(Collectors.toList());\n    }", "public List<DocumentResponse> findByRelatedEntity(EntityType entityType, Long entityId) {\n        return documentRepository.findByRelatedEntity(entityType, entityId).stream()\n                .map(this::toResponse)\n                .collect(Collectors.toList());\n    }\n\n    @Override\n    @Transactional(readOnly = true)\n    public Page<DocumentResponse> findByRelatedEntity(EntityType entityType, Long entityId, Pageable pageable) {\n        return documentRepository.findByRelatedEntity(entityType, entityId, pageable).map(this::toResponse);\n    }")
with open(path, "w") as f: f.write(c)
