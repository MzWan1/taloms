import re
path = "src/main/resources/templates/fragments/pagination.html"
with open(path, "r") as f: c = f.read()

c = c.replace('<div th:fragment="pagination(page, baseUrl)" th:if="${page.totalPages > 1}">', '<div th:fragment="pagination(page, baseUrl)">')
c = c.replace('<nav aria-label="Page navigation" class="mt-4">', '<nav aria-label="Page navigation" class="mt-4" th:if="${page.totalPages > 1}">')

with open(path, "w") as f: f.write(c)
print("Patched pagination")
