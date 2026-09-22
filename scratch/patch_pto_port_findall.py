path = "src/main/java/za/co/taloms/pto/domain/repository/PTORepositoryPort.java"
with open(path, "r") as f: c = f.read()

c = c.replace("List<PTO> findAll();", "List<PTO> findAll();\n    Page<PTO> findAll(Pageable pageable);")
with open(path, "w") as f: f.write(c)
