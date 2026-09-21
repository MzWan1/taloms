import sys

with open("src/main/resources/templates/ptos/list.html", "r") as f:
    content = f.read()

content = content.replace(
    """<div th:replace="~{fragments/pagination :: pagination(page=${page}, baseUrl='/ptos', queryParams=${queryParams})}"></div>""",
    ""
)

content = content.replace(
    """<tr th:each="pto : ${page.content}\"""",
    """<tr th:each="pto : ${ptos}\""""
)

with open("src/main/resources/templates/ptos/list.html", "w") as f:
    f.write(content)
