path = "src/main/java/za/co/taloms/pto/application/dto/PTOSearchCriteria.java"
with open(path, "r") as f: c = f.read()

if "java.util.Set" not in c:
    c = c.replace("import za.co.taloms.pto.domain.entity.PTOStatus;", "import za.co.taloms.pto.domain.entity.PTOStatus;\nimport java.util.Set;")

c = c.replace("private Long       villageId;", "private Set<Long> villageIds;")

with open(path, "w") as f: f.write(c)
