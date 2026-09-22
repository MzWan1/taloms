path = "src/main/java/za/co/taloms/dashboard/presentation/DashboardController.java"
with open(path, "r") as f: c = f.read()

import re

c = c.replace("import za.co.taloms.dashboard.application.service.DashboardService;", "import za.co.taloms.dashboard.application.service.DashboardService;\nimport za.co.taloms.dashboard.application.service.DashboardChartService;\nimport za.co.taloms.traditionalauthority.application.service.AuthorityScopeService;\nimport org.springframework.web.bind.annotation.ResponseBody;")

c = c.replace("    private final DashboardService dashboardService;", "    private final DashboardService dashboardService;\n    private final DashboardChartService dashboardChartService;\n    private final AuthorityScopeService authorityScopeService;")

chart_endpoint = """
    @GetMapping("/api/dashboard/chart")
    @ResponseBody
    public DashboardChartDto getChartData(Authentication authentication) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        java.util.Set<Long> scopedVillageIds = isAdmin ? null : authorityScopeService.scopedVillageIds();
        return dashboardChartService.getChartData(isAdmin, scopedVillageIds);
    }
"""

if "/api/dashboard/chart" not in c:
    c = c.replace("    @GetMapping(\"/api/dashboard/summary\")", chart_endpoint + "\n    @GetMapping(\"/api/dashboard/summary\")")

with open(path, "w") as f: f.write(c)
