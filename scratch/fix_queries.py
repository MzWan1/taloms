path = "src/main/java/za/co/taloms/audit/infrastructure/repository/AuditLogJpaRepository.java"
with open(path, "r") as f: c = f.read()

c = c.replace("List<AuditLog> findByEntityOrderByPerformedAtDesc(@Param(\"entityType\") String entityType, @Param(\"entityId\") Long entityId);\n    Page<AuditLog> findByEntityOrderByPerformedAtDesc(@Param(\"entityType\") String entityType, @Param(\"entityId\") Long entityId, Pageable pageable);", "List<AuditLog> findByEntityOrderByPerformedAtDesc(@Param(\"entityType\") String entityType, @Param(\"entityId\") Long entityId);\n\n    @Query(\"SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.performedAt DESC\")\n    Page<AuditLog> findByEntityOrderByPerformedAtDesc(@Param(\"entityType\") String entityType, @Param(\"entityId\") Long entityId, Pageable pageable);")

with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/document/infrastructure/repository/DocumentJpaRepository.java"
with open(path, "r") as f: c = f.read()

c = c.replace("List<Document> findByRelatedEntityOrderByUploadedAtDesc(@Param(\"entityType\") EntityType entityType,\n                                                            @Param(\"entityId\") Long entityId);\n    Page<Document> findByRelatedEntityOrderByUploadedAtDesc(@Param(\"entityType\") EntityType entityType,\n                                                            @Param(\"entityId\") Long entityId, Pageable pageable);", "List<Document> findByRelatedEntityOrderByUploadedAtDesc(@Param(\"entityType\") EntityType entityType,\n                                                            @Param(\"entityId\") Long entityId);\n\n    @Query(\"SELECT d FROM Document d WHERE d.relatedEntityType = :entityType AND d.relatedEntityId = :entityId ORDER BY d.uploadedAt DESC\")\n    Page<Document> findByRelatedEntityOrderByUploadedAtDesc(@Param(\"entityType\") EntityType entityType,\n                                                            @Param(\"entityId\") Long entityId, Pageable pageable);")

with open(path, "w") as f: f.write(c)
