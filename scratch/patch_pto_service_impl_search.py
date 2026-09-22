path = "src/main/java/za/co/taloms/pto/application/service/PTOServiceImpl.java"
with open(path, "r") as f: c = f.read()

c = c.replace("criteria.getVillageId(),", "criteria.getVillageIds(),")

with open(path, "w") as f: f.write(c)
