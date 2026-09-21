import sys

with open("src/main/resources/templates/dashboard/admin.html", "r") as f:
    content = f.read()

target = """                                    <tr th:if="${recentActivity == null or #lists.isEmpty(recentActivity)}">
                                        <td colspan="4" class="text-center py-4 text-muted">
                                            No recent activity
                                        <td colspan="4" class="text-center py-5">
                                            <div class="text-muted mb-3">There is currently no recent system activity recorded.</div>
                                            <a th:href="@{/audit}" class="btn btn-outline-primary btn-sm">View All Logs</a>
                                        </td>
                                    </tr>"""

replacement = """                                    <tr th:if="${recentActivity == null or #lists.isEmpty(recentActivity)}">
                                        <td colspan="4" class="text-center py-5">
                                            <div class="text-muted mb-3">There is currently no recent system activity recorded.</div>
                                            <a th:href="@{/audit}" class="btn btn-outline-primary btn-sm">View All Logs</a>
                                        </td>
                                    </tr>"""

content = content.replace(target, replacement)

with open("src/main/resources/templates/dashboard/admin.html", "w") as f:
    f.write(content)
