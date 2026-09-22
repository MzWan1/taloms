path = "src/main/java/za/co/taloms/pto/domain/repository/PTORepositoryPort.java"
with open(path, "r") as f: c = f.read()

c = c.replace("Page<PTO> findDeleted(Pageable pageable);", "Page<PTO> findDeleted(Pageable pageable);\n    Page<PTO> findDeletedScoped(Set<Long> villageIds, Pageable pageable);")

with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/pto/infrastructure/repository/PTORepositoryAdapter.java"
with open(path, "r") as f: c = f.read()

c = c.replace("public Page<PTO> findDeleted(Pageable pageable) {\n        return jpaRepository.findDeleted(pageable);\n    }", "public Page<PTO> findDeleted(Pageable pageable) {\n        return jpaRepository.findDeleted(pageable);\n    }\n\n    @Override\n    public Page<PTO> findDeletedScoped(Set<Long> villageIds, Pageable pageable) {\n        return jpaRepository.findDeletedScoped(villageIds != null && villageIds.isEmpty() ? null : villageIds, pageable);\n    }")

with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/pto/application/service/PTOService.java"
with open(path, "r") as f: c = f.read()
c = c.replace("Page<PTOResponse> findDeleted(Pageable pageable);", "Page<PTOResponse> findDeleted(Pageable pageable);\n    Page<PTOResponse> findDeletedScoped(Set<Long> villageIds, Pageable pageable);")
with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/pto/application/service/PTOServiceImpl.java"
with open(path, "r") as f: c = f.read()
c = c.replace("public Page<PTOResponse> findDeleted(Pageable pageable) {\n        return repositoryPort.findDeleted(pageable).map(ptoMapper::toDto);\n    }", "public Page<PTOResponse> findDeleted(Pageable pageable) {\n        return repositoryPort.findDeleted(pageable).map(ptoMapper::toDto);\n    }\n\n    @Override\n    @Transactional(readOnly = true)\n    public Page<PTOResponse> findDeletedScoped(Set<Long> villageIds, Pageable pageable) {\n        return repositoryPort.findDeletedScoped(villageIds, pageable).map(ptoMapper::toDto);\n    }")
with open(path, "w") as f: f.write(c)

