import sys

with open("src/main/resources/templates/portal/index.html", "r") as f:
    content = f.read()

# Tab 1 pagination
target_tab1 = """            </div>
        </div>
<!-- ========================== TAB 2: MANAGE ACCESS ========================== -->"""

replacement_tab1 = """            </div>
            <div th:if="${accessiblePage != null and accessiblePage.totalPages > 0}" class="py-3">
                <div th:replace="~{fragments/pagination :: pagination(page=${accessiblePage}, url='/portal', queryParams='tab=my')}"></div>
            </div>
        </div>
<!-- ========================== TAB 2: MANAGE ACCESS ========================== -->"""

content = content.replace(target_tab1, replacement_tab1)

# Tab 2 pagination
target_tab2 = """                            </div>
                        </div>
                    </div>
                </div>
<div th:if="${selectedPto != null}">"""

replacement_tab2 = """                            </div>
                        </div>
                        <div th:if="${ownedPage != null and ownedPage.totalPages > 0}" class="py-3 mt-3 border-top">
                            <div th:replace="~{fragments/pagination :: pagination(page=${ownedPage}, url='/portal', queryParams='tab=manage')}"></div>
                        </div>
                    </div>
                </div>
<div th:if="${selectedPto != null}">"""

content = content.replace(target_tab2, replacement_tab2)

with open("src/main/resources/templates/portal/index.html", "w") as f:
    f.write(content)

