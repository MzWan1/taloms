import glob

js_snippet = """
<script>
    document.addEventListener('DOMContentLoaded', function() {
        const form = document.querySelector('form[method="get"]');
        if (!form) return;
        
        const inputs = form.querySelectorAll('select, input[type="radio"], input[type="checkbox"]');
        inputs.forEach(input => {
            input.addEventListener('change', () => {
                form.submit();
            });
        });

        const searchInput = form.querySelector('input[type="text"][name="search"]');
        if (searchInput) {
            let timer;
            searchInput.addEventListener('input', () => {
                clearTimeout(timer);
                timer = setTimeout(() => {
                    form.submit();
                }, 500);
            });
            // Move cursor to end of input to maintain focus nicely on reload
            const val = searchInput.value;
            searchInput.value = '';
            searchInput.value = val;
            searchInput.focus();
        }
    });
</script>
</body>
"""

for path in ["src/main/resources/templates/users/list.html", "src/main/resources/templates/authorities/list.html", "src/main/resources/templates/companies/list.html", "src/main/resources/templates/ptos/list.html", "src/main/resources/templates/villages/list.html"]:
    with open(path, "r") as f: c = f.read()
    if 'document.addEventListener(\'DOMContentLoaded\', function() {' not in c:
        c = c.replace('</body>', js_snippet)
        with open(path, "w") as f: f.write(c)
        print("Added autosubmit to", path)
