path = "src/main/resources/templates/fragments/pagination.html"
with open(path, "r") as f: c = f.read()

c = c.replace("search=${param.search}, status=${param.status}", "q=${param.q}, search=${param.search}, status=${param.status}, villageId=${param.villageId}, documentType=${param.documentType}, entityType=${param.entityType}")

with open(path, "w") as f: f.write(c)
