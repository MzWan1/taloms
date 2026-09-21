import re

with open('./src/main/resources/templates/portal/index.html', 'r') as f:
    content = f.read()

# Add pagination for accessiblePtos
old_accessible = """            <div th:unless="${#lists.isEmpty(accessiblePtos)}" class="row g-3">
                <div class="col-12" th:each="pto : ${accessiblePtos}">"""
new_accessible = """            <div th:unless="${#lists.isEmpty(accessiblePtos)}" class="row g-3">
                <div class="col-12" th:each="pto : ${accessiblePtos}">"""

# Wait, we need to add the pagination div after the loop.
# Let's find the end of the `row g-3` for accessiblePtos.
# Actually, I'll just append it to the end of myPane if it exists.

# Let's check the myPane structure:
#             <div th:unless="${#lists.isEmpty(accessiblePtos)}" class="row g-3">
#                  ...
#             </div>
#         </div> <!-- end myPane -->

# Let's add the fragment replace.
import os
