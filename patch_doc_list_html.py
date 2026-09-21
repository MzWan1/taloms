import re

with open('./src/main/resources/templates/documents/list.html', 'r') as f:
    content = f.read()

old_empty = """<tr th:if="${documents == null or documents.empty}">
                        <td colspan="9" class="text-center py-5 text-muted">
                            <i class="bi bi-files display-5 d-block mb-2 opacity-25"></i>
                            <span class="fw-semibold">No documents found</span><br>
                            <small>Click "Upload Document" to upload your first file</small>
                        </td>
                    </tr>"""
new_empty = """<tr th:if="${page == null or page.empty}">
                        <td colspan="9" class="text-center py-5 text-muted">
                            <i class="bi bi-files display-5 d-block mb-2 opacity-25"></i>
                            <span class="fw-semibold">No documents found</span><br>
                            <a href="#" class="btn btn-navy mt-3" data-bs-toggle="modal" data-bs-target="#uploadDocumentModal">[Upload Document]</a>
                        </td>
                    </tr>"""

content = content.replace(old_empty, new_empty)

old_table_end = """            </div>
        </div>
    </div>
</div>"""
new_table_end = """            </div>
        </div>
        <div th:if="${page != null and page.totalPages > 0}" class="card-footer bg-white py-3">
            <div th:replace="~{fragments/pagination :: pagination(page=${page}, url='/documents', queryParams='')}"></div>
        </div>
    </div>
</div>"""

content = content.replace(old_table_end, new_table_end)

with open('./src/main/resources/templates/documents/list.html', 'w') as f:
    f.write(content)
