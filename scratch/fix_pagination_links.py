path = "src/main/resources/templates/fragments/pagination.html"
with open(path, "r") as f: c = f.read()

c = c.replace("page=${page.number - 1}", "page=${page.number}")
c = c.replace("page=${i}, size=${page.size}", "page=${i + 1}, size=${page.size}")
c = c.replace("page=${page.number + 1}", "page=${page.number + 2}")

with open(path, "w") as f: f.write(c)
