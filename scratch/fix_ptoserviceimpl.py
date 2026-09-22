path = "src/main/java/za/co/taloms/pto/application/service/PTOServiceImpl.java"
with open(path, "r") as f: c = f.read()

c = c.replace("repositoryPort.findAll(pageable).map(ptoMapper::toDto)", "ptoRepository.findAll(pageable).map(this::toResponse)")
c = c.replace("repositoryPort.search(", "ptoRepository.search(")
c = c.replace("pageable\n        ).map(ptoMapper::toDto)", "pageable\n        ).map(this::toResponse)")
c = c.replace("repositoryPort.findByTraditionalAuthorityId(authorityId, pageable).map(ptoMapper::toDto)", "ptoRepository.findByTraditionalAuthorityId(authorityId, pageable).map(this::toResponse)")
c = c.replace("repositoryPort.findDeleted(pageable).map(ptoMapper::toDto)", "ptoRepository.findDeleted(pageable).map(this::toResponse)")
c = c.replace("repositoryPort.findDeletedScoped(villageIds, pageable).map(ptoMapper::toDto)", "ptoRepository.findDeletedScoped(villageIds, pageable).map(this::toResponse)")

with open(path, "w") as f: f.write(c)
