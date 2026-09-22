path = "src/main/resources/templates/fragments/pagination.html"
with open(path, "r") as f: c = f.read()

import re

# Add class to the fragment div
c = c.replace('<div th:fragment="pagination(page, baseUrl)">', '<div th:fragment="pagination(page, baseUrl)" class="taloms-pagination-wrapper">')

# Replace the selector logic
old_selector = r"""                        // Find the table's card container
                        let tableCard = document\.querySelector\('\.card:has\(table\.table\)'\);
                        if \(!tableCard\) \{
                            // Fallback for older browsers
                            const tables = document\.querySelectorAll\('table\.table'\);
                            if \(tables\.length > 0\) \{
                                tableCard = tables\[0\]\.closest\('\.card'\);
                            \}
                        \}"""

new_selector = """                        // Find the card containing this specific pagination block
                        let tableCard = link.closest('.card');
                        if (!tableCard) {
                            tableCard = link.closest('.taloms-pagination-wrapper').parentElement;
                        }"""

c = re.sub(old_selector, new_selector, c)

old_new_selector = r"""                                let newTableCard = doc\.querySelector\('\.card:has\(table\.table\)'\);
                                if \(!newTableCard\) \{
                                    const tables = doc\.querySelectorAll\('table\.table'\);
                                    if \(tables\.length > 0\) \{
                                        newTableCard = tables\[0\]\.closest\('\.card'\);
                                    \}
                                \}"""

new_new_selector = """                                let newTableCard;
                                const newWrapper = doc.querySelector('.taloms-pagination-wrapper');
                                if (newWrapper) {
                                    newTableCard = newWrapper.closest('.card');
                                    if (!newTableCard) newTableCard = newWrapper.parentElement;
                                }"""

c = re.sub(old_new_selector, new_new_selector, c)

with open(path, "w") as f: f.write(c)

