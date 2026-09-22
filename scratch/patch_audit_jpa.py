path = "src/main/java/za/co/taloms/audit/infrastructure/repository/AuditLogJpaRepository.java"
with open(path, "r") as f: c = f.read()

if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.List;", "import java.util.List;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;")

c = c.replace("List<AuditLog> findAllOrderByPerformedAtDesc();", "List<AuditLog> findAllOrderByPerformedAtDesc();\n    Page<AuditLog> findAllOrderByPerformedAtDesc(Pageable pageable);")
c = c.replace("List<AuditLog> findByEntityOrderByPerformedAtDesc(@Param(\"entityType\") String entityType, @Param(\"entityId\") Long entityId);", "List<AuditLog> findByEntityOrderByPerformedAtDesc(@Param(\"entityType\") String entityType, @Param(\"entityId\") Long entityId);\n    Page<AuditLog> findByEntityOrderByPerformedAtDesc(@Param(\"entityType\") String entityType, @Param(\"entityId\") Long entityId, Pageable pageable);")

with open(path, "w") as f: f.write(c)
