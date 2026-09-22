path = "src/main/java/za/co/taloms/document/infrastructure/repository/DocumentJpaRepository.java"
with open(path, "r") as f: c = f.read()
c = c.replace("List<Document> findAllOrderByUploadedAtDesc();\n    Page<Document> findAllOrderByUploadedAtDesc(Pageable pageable);", "List<Document> findAllOrderByUploadedAtDesc();\n\n    @Query(\"SELECT d FROM Document d ORDER BY d.uploadedAt DESC\")\n    Page<Document> findAllOrderByUploadedAtDesc(Pageable pageable);")
with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/audit/infrastructure/repository/AuditLogJpaRepository.java"
with open(path, "r") as f: c = f.read()
c = c.replace("List<AuditLog> findAllOrderByPerformedAtDesc();\n    Page<AuditLog> findAllOrderByPerformedAtDesc(Pageable pageable);", "List<AuditLog> findAllOrderByPerformedAtDesc();\n\n    @Query(\"SELECT a FROM AuditLog a ORDER BY a.performedAt DESC\")\n    Page<AuditLog> findAllOrderByPerformedAtDesc(Pageable pageable);")
with open(path, "w") as f: f.write(c)
