import re

with open('./src/main/resources/templates/parcels/list.html', 'r') as f:
    content = f.read()

# Update empty state
old_empty = """<tr id="noParcelsRow" th:if="${parcels == null or parcels.empty}">
                        <td colspan="7" class="text-center py-5 text-muted">
                            <i class="bi bi-map display-5 d-block mb-2 opacity-25"></i>
                            <span class="fw-semibold">No parcels found</span><br>
                            <small>Click "New Parcel" to create the first record</small>
                        </td>
                    </tr>"""
new_empty = """<tr id="noParcelsRow" th:if="${page == null or page.empty}">
                        <td colspan="7" class="text-center py-5 text-muted">
                            <i class="bi bi-map display-5 d-block mb-2 opacity-25"></i>
                            <span class="fw-semibold">No parcels found</span><br>
                            <a th:href="@{/parcels/create}" class="btn btn-navy mt-3">[Add Parcel]</a>
                        </td>
                    </tr>"""

content = content.replace(old_empty, new_empty)

# Update the table closing tags to add pagination fragment
old_table_end = """            </div>
        </div>
    </div>
</div>"""
new_table_end = """            </div>
        </div>
        <div th:if="${page != null and page.totalPages > 0}" class="card-footer bg-white py-3">
            <div th:replace="~{fragments/pagination :: pagination(page=${page}, url='/parcels', queryParams=${'q=' + (q != null ? q : '') + '&status=' + (selectedStatus != null ? selectedStatus : '') + '&villageId=' + (selectedVillageId != null ? selectedVillageId : '')})}"></div>
        </div>
    </div>
</div>"""

content = content.replace(old_table_end, new_table_end)

with open('./src/main/resources/templates/parcels/list.html', 'w') as f:
    f.write(content)
