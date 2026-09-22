path = "src/main/java/za/co/taloms/pto/infrastructure/repository/PTOJpaRepository.java"
with open(path, "r") as f: c = f.read()

c = c.replace("Page<PTO> findDeleted(Pageable pageable);", "Page<PTO> findDeleted(Pageable pageable);\n\n    @Query(\"SELECT p FROM PTO p WHERE p.deletedAt IS NOT NULL AND (COALESCE(:villageIds, NULL) IS NULL OR p.village.id IN :villageIds) ORDER BY p.deletedAt DESC\")\n    Page<PTO> findDeletedScoped(@Param(\"villageIds\") Set<Long> villageIds, Pageable pageable);")

with open(path, "w") as f: f.write(c)
