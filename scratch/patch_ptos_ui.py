path = "src/main/resources/templates/ptos/list.html"
with open(path, "r") as f: c = f.read()

import re

# Remove the old pagination fragment
c = re.sub(r'    <!-- Pagination -->\s*<div th:replace="~\{fragments/pagination :: pagination\(page=\$\{page\}, baseUrl=\'/ptos\'\)\}"></div>\s*', '', c)

# Insert it at the end of the main card body
if 'class="card-footer bg-white border-top-0 py-3"' not in c:
    c = c.replace('</table>\n                        \n            </div>\n        </div>\n    </div>\n</div>', '</table>\n                        \n            </div>\n            <div class="card-footer bg-white border-top-0 py-3">\n                <div th:replace="~{fragments/pagination :: pagination(page=${page}, baseUrl=\'/ptos\')}"></div>\n            </div>\n        </div>\n    </div>\n</div>')

with open(path, "w") as f: f.write(c)
