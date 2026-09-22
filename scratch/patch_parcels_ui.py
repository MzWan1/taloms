path = "src/main/resources/templates/parcels/list.html"
with open(path, "r") as f: c = f.read()

if "fragments/pagination" not in c:
    c = c.replace("</table>\n            </div>\n        </div>\n    </div>", "</table>\n            </div>\n            <div class=\"card-footer bg-white border-top-0 py-3\">\n                <div th:replace=\"~{fragments/pagination :: pagination(page=${page}, baseUrl=${'/parcels'})}\"></div>\n            </div>\n        </div>\n    </div>")

with open(path, "w") as f: f.write(c)
