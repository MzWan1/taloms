path = "src/main/resources/templates/documents/list.html"
with open(path, "r") as f: c = f.read()
if "fragments/pagination" not in c:
    c = c.replace("</table>\n            </div>\n        </div>\n    </div>", "</table>\n            </div>\n            <div class=\"card-footer bg-white border-top-0 py-3\">\n                <div th:replace=\"~{fragments/pagination :: pagination}\"></div>\n            </div>\n        </div>\n    </div>")
with open(path, "w") as f: f.write(c)

path = "src/main/resources/templates/audit/index.html"
with open(path, "r") as f: c = f.read()
if "fragments/pagination" not in c:
    c = c.replace("</table>\n            </div>\n        </div>\n    </div>", "</table>\n            </div>\n            <div class=\"card-footer bg-white border-top-0 py-3\">\n                <div th:replace=\"~{fragments/pagination :: pagination}\"></div>\n            </div>\n        </div>\n    </div>")
with open(path, "w") as f: f.write(c)
