sed -i '' 's|<th>Last Used</th>|<th>Last Used</th>\n                                    <th>Requests</th>|g' src/main/resources/templates/company/dashboard.html
sed -i '' 's|<td class="text-end px-4">|<td th:text="${key.totalRequests != null ? key.totalRequests : 0}">0</td>\n                                    <td class="text-end px-4">|g' src/main/resources/templates/company/dashboard.html
