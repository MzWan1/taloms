path = "src/main/resources/templates/documents/list.html"
with open(path, "r") as f: c = f.read()
c = c.replace("<div th:replace=\"~{fragments/pagination :: pagination}\"></div>", "<div th:replace=\"~{fragments/pagination :: pagination(page=${page}, baseUrl=${'/documents'})}\"></div>")
with open(path, "w") as f: f.write(c)

path = "src/main/resources/templates/audit/index.html"
with open(path, "r") as f: c = f.read()
c = c.replace("<div th:replace=\"~{fragments/pagination :: pagination}\"></div>", "<div th:replace=\"~{fragments/pagination :: pagination(page=${page}, baseUrl=${'/audit'})}\"></div>")
with open(path, "w") as f: f.write(c)
