path = "src/main/resources/templates/dashboard/admin.html"
with open(path, "r") as f: c = f.read()

import re

# 1. Add the Chart block
chart_html = """
        <!-- Chart -->
        <div class="row g-3 mb-4">
            <div class="col-12">
                <div class="card border-0 shadow-sm">
                    <div class="card-header bg-white border-0 d-flex justify-content-between align-items-center py-3">
                        <h6 class="fw-bold text-navy mb-0">System Activity</h6>
                        <select id="chartCategorySelect" class="form-select form-select-sm w-auto" onchange="renderSystemActivityChart(this.value)">
                            <option value="All">All Categories</option>
                            <option value="Users Added">Users Added</option>
                            <option value="Authorities Added">Authorities Added</option>
                        </select>
                    </div>
                    <div class="card-body py-2">
                        <canvas id="systemActivityChart" style="max-height: 250px; width: 100%;"></canvas>
                    </div>
                </div>
            </div>
        </div>
"""

c = c.replace("<!-- Recent Activity -->", chart_html + "\n        <!-- Recent Activity -->")

# 2. Limit recent activity to 5 visually (to strictly adhere to prompt if somehow it has more)
c = c.replace('<tr th:each="activity : ${recentActivity}" th:unless="${recentActivity == null or #lists.isEmpty(recentActivity)}">', '<tr th:each="activity, iterStat : ${recentActivity}" th:if="${iterStat.index < 5}" th:unless="${recentActivity == null or #lists.isEmpty(recentActivity)}">')

# 3. Add Chart.js and script
js_code = """
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script>
        let systemActivityChart = null;
        let systemActivityData = null;

        function renderSystemActivityChart(category) {
            if (!systemActivityData) return;
            
            let datasets = [];
            if (category === 'All') {
                let curAgg = [0,0,0,0,0,0,0];
                let prevAgg = [0,0,0,0,0,0,0];
                for (let k in systemActivityData.currentWeek) {
                    systemActivityData.currentWeek[k].forEach((v, i) => curAgg[i] += v);
                    systemActivityData.previousWeek[k].forEach((v, i) => prevAgg[i] += v);
                }
                datasets.push({
                    label: 'This Week (Total)',
                    data: curAgg,
                    borderColor: '#0d6efd',
                    tension: 0.3
                });
                datasets.push({
                    label: 'Previous Week (Total)',
                    data: prevAgg,
                    borderColor: '#adb5bd',
                    borderDash: [5, 5],
                    tension: 0.3
                });
            } else {
                datasets.push({
                    label: 'This Week (' + category + ')',
                    data: systemActivityData.currentWeek[category] || [0,0,0,0,0,0,0],
                    borderColor: '#0d6efd',
                    tension: 0.3
                });
                datasets.push({
                    label: 'Previous Week (' + category + ')',
                    data: systemActivityData.previousWeek[category] || [0,0,0,0,0,0,0],
                    borderColor: '#adb5bd',
                    borderDash: [5, 5],
                    tension: 0.3
                });
            }

            if (systemActivityChart) systemActivityChart.destroy();
            systemActivityChart = new Chart(document.getElementById('systemActivityChart'), {
                type: 'line',
                data: {
                    labels: systemActivityData.labels,
                    datasets: datasets
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    scales: {
                        y: { beginAtZero: true, ticks: { precision: 0 } }
                    }
                }
            });
        }

        document.addEventListener('DOMContentLoaded', function() {
            fetch('/api/dashboard/chart')
                .then(r => r.json())
                .then(data => {
                    systemActivityData = data;
                    renderSystemActivityChart('All');
                });
        });
    </script>
"""

c = c.replace("</body>", js_code + "\n</body>")

with open(path, "w") as f: f.write(c)

