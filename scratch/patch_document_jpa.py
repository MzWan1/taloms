path = "src/main/java/za/co/taloms/document/infrastructure/repository/DocumentJpaRepository.java"
with open(path, "r") as f: c = f.read()

if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.Optional;", "import java.util.Optional;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;")

c = c.replace("List<Document> findAllOrderByUploadedAtDesc();", "List<Document> findAllOrderByUploadedAtDesc();\n    Page<Document> findAllOrderByUploadedAtDesc(Pageable pageable);")
c = c.replace("List<Document> findByRelatedEntityOrderByUploadedAtDesc(@Param(\"entityType\") EntityType entityType,\n                                                            @Param(\"entityId\") Long entityId);", "List<Document> findByRelatedEntityOrderByUploadedAtDesc(@Param(\"entityType\") EntityType entityType,\n                                                            @Param(\"entityId\") Long entityId);\n    Page<Document> findByRelatedEntityOrderByUploadedAtDesc(@Param(\"entityType\") EntityType entityType,\n                                                            @Param(\"entityId\") Long entityId, Pageable pageable);")

with open(path, "w") as f: f.write(c)
