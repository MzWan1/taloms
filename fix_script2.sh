sed -i '' '/<script>/,$d' src/main/resources/templates/companies/list.html
cat << 'INNER_EOF' >> src/main/resources/templates/companies/list.html
    <script>
        document.addEventListener('DOMContentLoaded', () => {
            const searchInput = document.getElementById('ownerSearchInput');
            const resultsContainer = document.getElementById('searchResults');
            const selectedContainer = document.getElementById('selectedUserContainer');
            const searchBoxContainer = document.getElementById('searchBoxContainer');
            const hiddenUserId = document.getElementById('ownerUserId');
            
            const nameLabel = document.getElementById('selectedUserFullName');
            const usernameLabel = document.getElementById('selectedUserUsername');
            const emailLabel = document.getElementById('selectedUserEmail');
            
            let debounceTimer = null;
            let lastQuery = '';

            searchInput.addEventListener('input', (e) => {
                const query = e.target.value.trim();
                if (query === lastQuery) return;
                
                clearTimeout(debounceTimer);
                
                if (query === '') {
                    resultsContainer.classList.add('d-none');
                    resultsContainer.innerHTML = '';
                    lastQuery = '';
                    return;
                }

                if (/^[0-9]+$/.test(query)) {
                    if (query.length === 13) {
                        lastQuery = query;
                        fetchResults(query);
                    } else {
                        resultsContainer.classList.add('d-none');
                    }
                    return;
                }
                
                if (query.length >= 3) {
                    debounceTimer = setTimeout(() => {
                        lastQuery = query;
                        fetchResults(query);
                    }, 400);
                } else {
                    resultsContainer.classList.add('d-none');
                }
            });

            async function fetchResults(query) {
                try {
                    const res = await fetch(`/api/companies/owners/search?q=${encodeURIComponent(query)}`);
                    if (!res.ok) throw new Error('Search failed');
                    const data = await res.json();
                    renderResults(data.data || []);
                } catch (err) {
                    console.error(err);
                    resultsContainer.innerHTML = '<div class="list-group-item text-danger">Error fetching results.</div>';
                    resultsContainer.classList.remove('d-none');
                }
            }

            function renderResults(users) {
                resultsContainer.innerHTML = '';
                if (users.length === 0) {
                    resultsContainer.innerHTML = '<div class="list-group-item text-muted">No eligible users found.</div>';
                } else {
                    users.forEach(u => {
                        const btn = document.createElement('button');
                        btn.type = 'button';
                        btn.className = 'list-group-item list-group-item-action py-2';
                        
                        if (u.alreadyAssigned) {
                            btn.innerHTML = `
                                <div class="d-flex w-100 justify-content-between align-items-center opacity-50">
                                    <div>
                                        <strong class="d-block">${u.fullName}</strong>
                                        <small class="text-muted">Username: ${u.username} | Email: ${u.email}</small>
                                        <div class="small text-danger"><i class="bi bi-x-circle me-1"></i>Already linked to a company</div>
                                    </div>
                                    <span class="btn btn-sm btn-secondary px-3 disabled">Unavailable</span>
                                </div>
                            `;
                            btn.disabled = true;
                        } else {
                            btn.innerHTML = `
                                <div class="d-flex w-100 justify-content-between align-items-center">
                                    <div>
                                        <strong class="d-block">${u.fullName}</strong>
                                        <small class="text-muted">Username: ${u.username} | Email: ${u.email}</small>
                                        <div class="small text-muted">ID: ${u.maskedIdNumber || 'N/A'}</div>
                                    </div>
                                    <span class="btn btn-sm btn-primary px-3">Select</span>
                                </div>
                            `;
                            btn.onclick = () => selectUser(u);
                        }
                        
                        resultsContainer.appendChild(btn);
                    });
                }
                resultsContainer.classList.remove('d-none');
            }

            function selectUser(user) {
                hiddenUserId.value = user.id;
                nameLabel.textContent = user.fullName;
                usernameLabel.textContent = user.username;
                emailLabel.textContent = user.email;
                
                searchBoxContainer.classList.add('d-none');
                resultsContainer.classList.add('d-none');
                selectedContainer.classList.remove('d-none');
                searchInput.value = '';
            }

            window.clearSelectedUser = function() {
                hiddenUserId.value = '';
                selectedContainer.classList.add('d-none');
                searchBoxContainer.classList.remove('d-none');
                searchInput.focus();
            };
        });
    </script>
</body>
</html>
INNER_EOF
